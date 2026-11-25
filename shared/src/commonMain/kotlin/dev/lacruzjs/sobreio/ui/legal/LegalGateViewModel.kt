package dev.lacruzjs.sobreio.ui.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.LegalRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.loading.revalidate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Asks the signed-in user to accept the terms and the privacy policy in force before the app shows (D-57). One
 * mechanism for every case: a new account (email or Google), a user of an earlier version and a new version of a
 * document. A failed read lets the user in and the gate asks again on the next start: an outage or a database without
 * D-57 never locks anyone out of their data.
 */
class LegalGateViewModel(
    private val legal: LegalRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LegalGateUiState>(LegalGateUiState.Checking)
    val state: StateFlow<LegalGateUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: LegalGateEvent) {
        when (event) {
            is LegalGateEvent.AcceptedChanged -> updateRequired { it.copy(accepted = event.checked, error = null) }
            LegalGateEvent.ContinueClicked -> accept()
            LegalGateEvent.SignOutClicked -> signOut()
            is LegalGateEvent.OpenDocument -> Unit // The Route opens it
        }
    }

    private fun load() {
        viewModelScope.launch {
            revalidate(onRefreshing = {}) {
                legal.getStatus()
                    .onSuccess { statuses -> _state.value = legalGateStateOf(statuses) }
                    .onFailure { _state.value = LegalGateUiState.Clear }
            }
        }
    }

    private fun accept() {
        val current = _state.value as? LegalGateUiState.Required ?: return
        if (!current.canContinue) return
        updateRequired { it.copy(isAccepting = true, error = null) }
        viewModelScope.launch {
            legal.accept(current.terms.version, current.privacy.version)
                .onSuccess { _state.value = LegalGateUiState.Clear }
                .onFailure { e ->
                    val error = e.asAppError()
                    if (error is AppError.BusinessRule && error.code == VERSION_NOT_IN_FORCE) {
                        // A new version took effect while the screen was open: show it, unticked, with the reason
                        legal.getStatus().onSuccess { statuses ->
                            val fresh = legalGateStateOf(statuses)
                            _state.value = if (fresh is LegalGateUiState.Required) fresh.copy(error = error) else fresh
                        }.onFailure { updateRequired { it.copy(isAccepting = false, error = error) } }
                    } else {
                        updateRequired { it.copy(isAccepting = false, error = error) }
                    }
                }
        }
    }

    private fun signOut() {
        updateRequired { it.copy(isSigningOut = true, error = null) }
        viewModelScope.launch {
            // On success the session-driven root shows the sign-in screens by itself
            auth.signOut().onFailure { e -> updateRequired { it.copy(isSigningOut = false, error = e.asAppError()) } }
        }
    }

    private fun updateRequired(change: (LegalGateUiState.Required) -> LegalGateUiState.Required) {
        _state.update { if (it is LegalGateUiState.Required) change(it) else it }
    }

    private companion object {
        const val VERSION_NOT_IN_FORCE = "FZ025"
    }
}
