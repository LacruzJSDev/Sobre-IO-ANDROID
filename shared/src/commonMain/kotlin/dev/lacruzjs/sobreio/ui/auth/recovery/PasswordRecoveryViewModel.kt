package dev.lacruzjs.sobreio.ui.auth.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.auth.CODE_LENGTH
import dev.lacruzjs.sobreio.ui.auth.validateCode
import dev.lacruzjs.sobreio.ui.auth.validateEmail
import dev.lacruzjs.sobreio.ui.auth.validatePassword
import dev.lacruzjs.sobreio.ui.auth.validatePasswordsMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(PasswordRecoveryUiState())
    val state: StateFlow<PasswordRecoveryUiState> = _state.asStateFlow()

    fun onEvent(event: PasswordRecoveryEvent) {
        when (event) {
            is PasswordRecoveryEvent.EmailChanged ->
                _state.update { it.copy(email = event.value, emailError = null, error = null) }
            PasswordRecoveryEvent.SendCodeClicked -> sendCode(isResend = false)
            PasswordRecoveryEvent.ResendCodeClicked -> sendCode(isResend = true)
            PasswordRecoveryEvent.ChangeEmailClicked ->
                _state.update { it.copy(step = RecoveryStep.Email, code = "", codeError = null, codeResent = false, error = null) }
            is PasswordRecoveryEvent.CodeChanged -> _state.update {
                it.copy(
                    code = event.value.filter(Char::isDigit).take(CODE_LENGTH),
                    codeError = null,
                    codeResent = false,
                    error = null,
                )
            }
            is PasswordRecoveryEvent.NewPasswordChanged ->
                _state.update { it.copy(newPassword = event.value, newPasswordError = null, error = null) }
            is PasswordRecoveryEvent.RepeatedPasswordChanged ->
                _state.update { it.copy(repeatedPassword = event.value, repeatedPasswordError = null, error = null) }
            PasswordRecoveryEvent.ResetClicked -> reset()
            PasswordRecoveryEvent.BackToSignInClicked -> Unit // The Route navigates
        }
    }

    private fun sendCode(isResend: Boolean) {
        val current = _state.value
        if (current.isLoading) return
        val emailError = validateEmail(current.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null, codeResent = false) }
        viewModelScope.launch {
            auth.requestPasswordReset(current.email.trim())
                .onSuccess { _state.update { it.copy(isLoading = false, step = RecoveryStep.Reset, codeResent = isResend) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }

    private fun reset() {
        val current = _state.value
        if (current.isLoading) return
        val codeError = validateCode(current.code)
        val passwordError = validatePassword(current.newPassword)
        val repeatedError = validatePasswordsMatch(current.newPassword, current.repeatedPassword)
        if (codeError != null || passwordError != null || repeatedError != null) {
            _state.update {
                it.copy(codeError = codeError, newPasswordError = passwordError, repeatedPasswordError = repeatedError)
            }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the session changes and root navigation shows the app
            auth.resetPassword(current.email.trim(), current.code, current.newPassword)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }
}
