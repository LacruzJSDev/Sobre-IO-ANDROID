package dev.lacruzjs.sobreio.ui.auth.confirm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.auth.CODE_LENGTH
import dev.lacruzjs.sobreio.ui.auth.validateCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Seconds the user waits between two requests for a code; the server rate-limits them as well. */
const val RESEND_COOLDOWN_SECONDS = 60

class ConfirmEmailViewModel(
    email: String,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConfirmEmailUiState(email = email))
    val state: StateFlow<ConfirmEmailUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        // This screen is only reached right after a code was sent
        startCooldown()
    }

    fun onEvent(event: ConfirmEmailEvent) {
        when (event) {
            is ConfirmEmailEvent.CodeChanged -> _state.update {
                it.copy(
                    code = event.value.filter(Char::isDigit).take(CODE_LENGTH),
                    codeError = null,
                    codeResent = false,
                    error = null,
                )
            }
            ConfirmEmailEvent.ConfirmClicked -> confirm()
            ConfirmEmailEvent.ResendClicked -> resend()
            ConfirmEmailEvent.BackToSignInClicked -> Unit // The Route navigates
        }
    }

    private fun confirm() {
        val current = _state.value
        if (current.isLoading) return
        val codeError = validateCode(current.code)
        if (codeError != null) {
            _state.update { it.copy(codeError = codeError) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the session changes and root navigation shows the app
            auth.confirmSignUp(current.email, current.code)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }

    private fun resend() {
        val current = _state.value
        if (current.isResending || current.resendSecondsLeft > 0) return
        _state.update { it.copy(isResending = true, error = null, codeResent = false) }
        viewModelScope.launch {
            auth.resendSignUpCode(current.email)
                .onSuccess {
                    _state.update { it.copy(isResending = false, codeResent = true) }
                    startCooldown()
                }
                .onFailure { e -> _state.update { it.copy(isResending = false, error = e.asAppError()) } }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (left in RESEND_COOLDOWN_SECONDS downTo 0) {
                _state.update { it.copy(resendSecondsLeft = left) }
                if (left > 0) delay(1_000)
            }
        }
    }
}
