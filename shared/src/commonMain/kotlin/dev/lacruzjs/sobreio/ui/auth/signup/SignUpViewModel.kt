package dev.lacruzjs.sobreio.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.LegalRepository
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.ui.loading.revalidate
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.validation.validateEmail
import dev.lacruzjs.sobreio.ui.validation.validatePassword
import dev.lacruzjs.sobreio.ui.validation.validateRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val auth: AuthRepository,
    private val legal: LegalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    init {
        // The links are read without a session (D-57); without them the form simply shows no notice
        viewModelScope.launch {
            revalidate(onRefreshing = {}) {
                legal.getDocumentsInForce().onSuccess { documents ->
                    _state.update {
                        it.copy(
                            terms = documents.firstOrNull { d -> d.kind == LegalDocumentKind.Terms },
                            privacy = documents.firstOrNull { d -> d.kind == LegalDocumentKind.Privacy },
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.NameChanged -> _state.update { it.copy(name = event.value, nameError = null, error = null) }
            is SignUpEvent.EmailChanged -> _state.update { it.copy(email = event.value, emailError = null, error = null) }
            is SignUpEvent.PasswordChanged ->
                _state.update { it.copy(password = event.value, passwordError = null, error = null) }
            SignUpEvent.SignUpClicked -> signUp()
            SignUpEvent.NavigationHandled -> _state.update { it.copy(navigateToConfirm = null) }
            SignUpEvent.SignInClicked, is SignUpEvent.OpenDocument -> Unit // The Route handles them
        }
    }

    private fun signUp() {
        val current = _state.value
        if (current.isLoading) return
        val nameError = validateRequired(current.name)
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (nameError != null || emailError != null || passwordError != null) {
            _state.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val email = current.email.trim()
            auth.signUp(current.name.trim(), email, current.password)
                .onSuccess { _state.update { it.copy(isLoading = false, navigateToConfirm = email) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }
}
