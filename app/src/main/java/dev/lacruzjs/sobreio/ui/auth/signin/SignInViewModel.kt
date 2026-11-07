package dev.lacruzjs.sobreio.ui.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.auth.validateEmail
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val auth: AuthRepository,
    isGoogleAvailable: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState(isGoogleAvailable = isGoogleAvailable))
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.EmailChanged -> _state.update { it.copy(email = event.value, emailError = null, error = null) }
            is SignInEvent.PasswordChanged ->
                _state.update { it.copy(password = event.value, passwordError = null, error = null) }
            SignInEvent.SignInClicked -> signIn()
            SignInEvent.ConfirmEmailClicked -> resendCodeAndConfirm()
            is SignInEvent.GoogleTokenReceived -> signInWithGoogle(event)
            SignInEvent.GoogleFailed -> _state.update { it.copy(error = AppError.Unknown()) }
            SignInEvent.NavigationHandled -> _state.update { it.copy(navigateToConfirm = null) }
            // Handled by the Route, which owns the platform pieces and the back stack
            SignInEvent.GoogleClicked, SignInEvent.ForgotPasswordClicked, SignInEvent.CreateAccountClicked -> Unit
        }
    }

    private fun signIn() {
        val current = _state.value
        if (current.isLoading) return
        val emailError = validateEmail(current.email)
        val passwordError = validateRequired(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the session changes and root navigation replaces this screen
            auth.signIn(current.email.trim(), current.password)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }

    private fun resendCodeAndConfirm() {
        val current = _state.value
        if (current.isLoading) return
        val email = current.email.trim()
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            auth.resendSignUpCode(email)
                .onSuccess { _state.update { it.copy(isLoading = false, navigateToConfirm = email) } }
                .onFailure { e ->
                    val error = e.asAppError()
                    // Rate limited means a code was sent moments ago, so the code screen is still the right place
                    if (error == AppError.TooManyRequests) {
                        _state.update { it.copy(isLoading = false, navigateToConfirm = email) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = error) }
                    }
                }
        }
    }

    private fun signInWithGoogle(event: SignInEvent.GoogleTokenReceived) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            auth.signInWithGoogle(event.idToken, event.rawNonce)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }
}
