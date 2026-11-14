package dev.lacruzjs.sobreio.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class ProfileViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // The field takes the user's name once; later changes of the user must not overwrite what is being typed
    private var nameLoaded = false

    init {
        viewModelScope.launch {
            auth.authState.collect { authState ->
                val user = (authState as? AuthState.SignedIn)?.user
                _state.update {
                    val loadName = !nameLoaded && user != null
                    if (loadName) nameLoaded = true
                    it.copy(user = user, name = if (loadName) user?.displayName.orEmpty() else it.name)
                }
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, saveError = null, saved = false) }
            ProfileEvent.SaveClicked -> save()
            // Handled by the Route, which owns the back stack
            ProfileEvent.BackClicked -> Unit
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.user == null) return
        val name = current.name.trim()
        val nameError = validateName(name)
        if (nameError != null) {
            _state.update { it.copy(nameError = nameError) }
            return
        }
        _state.update { it.copy(isSaving = true, saveError = null, saved = false) }
        viewModelScope.launch {
            auth.updateDisplayName(name)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saved = true,
                            name = name,
                            user = it.user?.copy(displayName = name),
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isSaving = false, saveError = e.asAppError()) } }
        }
    }

    private fun validateName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_DISPLAY_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }
}
