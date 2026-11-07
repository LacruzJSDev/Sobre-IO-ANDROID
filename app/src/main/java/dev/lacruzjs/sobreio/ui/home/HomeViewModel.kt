package dev.lacruzjs.sobreio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.authState.collect { authState ->
                _state.update { it.copy(user = (authState as? AuthState.SignedIn)?.user) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.SignOutClicked -> signOut()
        }
    }

    private fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true, error = null) }
        viewModelScope.launch {
            // On success the session ends and root navigation shows sign-in
            auth.signOut()
                .onSuccess { _state.update { it.copy(isSigningOut = false) } }
                .onFailure { e -> _state.update { it.copy(isSigningOut = false, error = e.asAppError()) } }
        }
    }
}
