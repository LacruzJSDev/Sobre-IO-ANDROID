package dev.lacruzjs.sobreio.ui.groupform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.ui.groups.GroupFormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The create and edit form of a group. [groupId] null creates; otherwise the group is read and its fields filled in.
 * Saving creates (with every field) or updates, and asks the Route to leave through [GroupFormUiState.savedGroupId].
 */
class GroupFormViewModel(
    private val groupId: String?,
    private val groups: GroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupFormUiState(isLoading = groupId != null))
    val state: StateFlow<GroupFormUiState> = _state.asStateFlow()

    init {
        if (groupId != null) load(groupId)
    }

    fun onEvent(event: GroupFormEvent) {
        when (event) {
            is GroupFormEvent.FormChanged -> _state.update { it.copy(form = event.form, submitError = null) }
            is GroupFormEvent.TimeZonePicked ->
                _state.update { it.copy(form = it.form.copy(timezone = event.id), submitError = null) }
            GroupFormEvent.SaveClicked -> save()
            GroupFormEvent.Retry -> groupId?.let { load(it) }
            GroupFormEvent.NavigationHandled -> _state.update { it.copy(savedGroupId = null) }
            // Handled by the Route, which owns the back stack
            GroupFormEvent.CloseClicked, GroupFormEvent.TimeZoneClicked -> Unit
        }
    }

    private fun load(id: String) {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            groups.getGroup(id)
                .onSuccess { group -> _state.update { it.copy(form = GroupFormState.forEdit(group), isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.asAppError()) } }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading || current.loadError != null) return
        val form = current.form.validated()
        if (form.nameError != null) {
            _state.update { it.copy(form = form) }
            return
        }
        val name = form.name.trim()
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val editing = form.editing
            val result = if (editing != null) {
                groups.updateGroup(editing.id, name, form.timezone, form.periodStartDay, form.color, form.icon)
                    .map { editing.id }
            } else {
                groups.createGroup(name, form.currency, form.timezone, form.periodStartDay, form.color, form.icon)
            }
            result
                .onSuccess { id -> _state.update { it.copy(isSubmitting = false, savedGroupId = id) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }
}
