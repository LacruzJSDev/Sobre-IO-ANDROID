package dev.lacruzjs.sobreio.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class GroupsViewModel(private val groups: GroupRepository) : ViewModel() {

    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    fun onEvent(event: GroupsEvent) {
        when (event) {
            GroupsEvent.Refresh -> load()
            GroupsEvent.NewGroupClicked -> _state.update { it.openSheet(GroupsSheet.Create) }
            GroupsEvent.JoinClicked -> _state.update { it.openSheet(GroupsSheet.Join) }
            GroupsEvent.SheetDismissed -> _state.update { if (it.isSubmitting) it else it.copy(sheet = GroupsSheet.None) }
            is GroupsEvent.GroupNameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is GroupsEvent.CurrencySelected -> _state.update { it.copy(currency = event.code) }
            GroupsEvent.CreateConfirmed -> createGroup()
            is GroupsEvent.CodeChanged -> _state.update { it.copy(code = event.value, codeError = null, submitError = null) }
            GroupsEvent.JoinConfirmed -> joinGroup()
            GroupsEvent.NavigationHandled -> _state.update { it.copy(navigateToGroup = null) }
            // Handled by the Route, which owns the back stack
            GroupsEvent.ProfileClicked, is GroupsEvent.GroupClicked -> Unit
        }
    }

    private fun load() {
        viewModelScope.launch {
            groups.getGroups()
                .onSuccess { list -> _state.update { it.copy(groups = list, isLoading = false, error = null) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }

    private fun createGroup() {
        val current = _state.value
        if (current.isSubmitting) return
        val name = current.name.trim()
        val nameError = validateGroupName(name)
        if (nameError != null) {
            _state.update { it.copy(nameError = nameError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.createGroup(name, current.currency)
                .onSuccess { id -> _state.update { it.opened(id) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    private fun joinGroup() {
        val current = _state.value
        if (current.isSubmitting) return
        val code = current.code.trim()
        val codeError = validateRequired(code)
        if (codeError != null) {
            _state.update { it.copy(codeError = codeError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.joinGroup(code)
                .onSuccess { id -> _state.update { it.opened(id) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    private fun validateGroupName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_GROUP_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }

    private fun GroupsUiState.openSheet(sheet: GroupsSheet) = copy(
        sheet = sheet,
        name = "",
        nameError = null,
        currency = GroupCurrencies.first(),
        code = "",
        codeError = null,
        submitError = null,
    )

    private fun GroupsUiState.opened(groupId: String) =
        copy(isSubmitting = false, sheet = GroupsSheet.None, navigateToGroup = groupId)
}
