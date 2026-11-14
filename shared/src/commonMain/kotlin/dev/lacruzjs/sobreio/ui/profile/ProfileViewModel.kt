package dev.lacruzjs.sobreio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.authState.collect { authState ->
                _state.update { it.copy(user = (authState as? AuthState.SignedIn)?.user) }
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            // Handled by the Route, which owns the back stack
            ProfileEvent.BackClicked -> Unit
        }
    }
}
