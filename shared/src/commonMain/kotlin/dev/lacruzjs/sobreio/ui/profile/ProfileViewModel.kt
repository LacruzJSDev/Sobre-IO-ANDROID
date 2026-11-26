package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.ui.validation.validateName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.AppPreferencesStore
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.ExportRepository
import dev.lacruzjs.sobreio.data.repository.LegalRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.export_save_failed
import dev.lacruzjs.sobreio.generated.resources.export_saved
import dev.lacruzjs.sobreio.ui.export.SaveResult
import dev.lacruzjs.sobreio.ui.loading.revalidate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val auth: AuthRepository,
    private val preferences: AppPreferencesStore,
    private val legal: LegalRepository,
    private val export: ExportRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // The field takes the user's name once; later changes of the user must not overwrite what is being typed
    private var nameLoaded = false

    init {
        viewModelScope.launch {
            revalidate(onRefreshing = {}) {
                // Without the documents (none published, or a failure) the section shows only "Download my data"
                legal.getStatus().onSuccess { statuses -> _state.update { it.copy(legal = statuses) } }
            }
        }
        viewModelScope.launch {
            preferences.colorBlindSafe.collect { enabled -> _state.update { it.copy(colorBlindSafe = enabled) } }
        }
        viewModelScope.launch {
            auth.authState.collect { authState ->
                val user = (authState as? AuthState.SignedIn)?.user
                _state.update {
                    val loadName = !nameLoaded && user != null
                    if (loadName) nameLoaded = true
                    it.copy(user = user, name = if (loadName) user.displayName.orEmpty() else it.name)
                }
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, saveError = null, saved = false) }
            ProfileEvent.SaveClicked -> save()
            is ProfileEvent.ColorBlindSafeChanged -> preferences.setColorBlindSafe(event.enabled)
            ProfileEvent.DownloadDataClicked -> downloadData()
            is ProfileEvent.DataFileHandled -> _state.update {
                it.copy(
                    pendingData = null,
                    downloadMessage = when (event.result) {
                        SaveResult.Saved -> Res.string.export_saved
                        SaveResult.Failed -> Res.string.export_save_failed
                        SaveResult.Cancelled -> null
                    },
                )
            }
            // Handled by the Route, which owns the back stack and the browser
            ProfileEvent.BackClicked, ProfileEvent.DeleteAccountClicked, is ProfileEvent.OpenDocument -> Unit
        }
    }

    /** Reads everything of the user's (D-58) and hands it to the Route for the save dialog. Never cached: always fresh. */
    private fun downloadData() {
        val current = _state.value
        if (current.isDownloading || current.pendingData != null) return
        _state.update { it.copy(isDownloading = true, downloadMessage = null, downloadError = null) }
        viewModelScope.launch {
            export.exportMyData()
                .onSuccess { json ->
                    val day = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    _state.update { it.copy(isDownloading = false, pendingData = DataFile(day, json)) }
                }
                .onFailure { e -> _state.update { it.copy(isDownloading = false, downloadError = e.asAppError()) } }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.user == null) return
        val name = current.name.trim()
        val nameError = validateName(name, MAX_DISPLAY_NAME_LENGTH)
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

}
