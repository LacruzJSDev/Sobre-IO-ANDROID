package dev.lacruzjs.sobreio.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_deleted
import dev.lacruzjs.sobreio.generated.resources.group_left
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class GroupsViewModel(
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val lastGroup: LastGroupStore,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    fun onEvent(event: GroupsEvent) {
        when (event) {
            GroupsEvent.Refresh -> {
                // A leave still waiting for its snackbar must be final before the list is read again
                undo.commit()
                load()
            }
            GroupsEvent.NewGroupClicked -> _state.update { it.openSheet(GroupsSheet.Create) }
            is GroupsEvent.EditClicked -> edit(event.groupId)
            GroupsEvent.EditConfirmed -> rename()
            is GroupsEvent.DeleteClicked -> deleteOrLeave(event.groupId)
            GroupsEvent.UndoClicked -> undo.undo()
            GroupsEvent.UndoExpired -> undo.commit()
            GroupsEvent.SignOutClicked -> signOut()
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

    override fun onCleared() {
        // viewModelScope is cancelled by now; a leave the user did not undo still has to reach the database
        undo.commitDetached()
    }

    private fun load() {
        viewModelScope.launch {
            groups.getGroups()
                .onSuccess { list -> _state.update { it.copy(groups = list, isLoading = false, error = null) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.asAppError()) } }
        }
    }

    private fun signOut() {
        if (_state.value.isSigningOut) return
        _state.update { it.copy(isSigningOut = true, signOutError = null) }
        viewModelScope.launch {
            // On success the session ends and root navigation shows sign-in
            auth.signOut()
                .onSuccess { _state.update { it.copy(isSigningOut = false) } }
                .onFailure { e -> _state.update { it.copy(isSigningOut = false, signOutError = e.asAppError()) } }
        }
    }

    private fun edit(groupId: String) {
        val group = _state.value.groups.firstOrNull { it.id == groupId && it.canEdit } ?: return
        _state.update { it.openSheet(GroupsSheet.Edit).copy(editingGroup = group, name = group.name) }
    }

    private fun rename() {
        val current = _state.value
        val group = current.editingGroup ?: return
        if (current.isSubmitting) return
        val name = current.name.trim()
        val nameError = validateGroupName(name)
        if (nameError != null) {
            _state.update { it.copy(nameError = nameError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.renameGroup(group.id, name)
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            isSubmitting = false,
                            sheet = GroupsSheet.None,
                            editingGroup = null,
                            groups = s.groups.map { if (it.id == group.id) it.copy(name = name) else it },
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /**
     * No confirmation: the group disappears at once and a snackbar offers "Undo". An owner's deletion is requested
     * right away (the database keeps the group for 7 days and "Undo" cancels it). Leaving cannot be undone in the
     * database, so it waits: it is sent when the snackbar ends without "Undo".
     */
    private fun deleteOrLeave(groupId: String) {
        val group = _state.value.groups.firstOrNull { it.id == groupId } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update { it.copy(groups = it.groups - group, actionError = null) }
        val restore = { _state.update { it.copy(groups = (it.groups + group).sortedBy { g -> g.name.lowercase() }) } }

        if (group.isOwner) {
            viewModelScope.launch {
                groups.requestGroupDeletion(group.id)
                    .onSuccess {
                        undo.show(
                            UndoableAction(
                                message = Res.string.group_deleted,
                                argument = group.name,
                                commit = { forgetAsLastGroup(group.id); Result.success(Unit) },
                                undo = { groups.cancelGroupDeletion(group.id) },
                                restore = { restore() },
                            ),
                        )
                    }
                    .onFailure { e ->
                        restore()
                        _state.update { it.copy(actionError = e.asAppError()) }
                    }
            }
        } else {
            undo.show(
                UndoableAction(
                    message = Res.string.group_left,
                    argument = group.name,
                    commit = { groups.leaveGroup(group.id).onSuccess { forgetAsLastGroup(group.id) } },
                    undo = { Result.success(Unit) },
                    restore = { restore() },
                ),
            )
        }
    }

    private suspend fun forgetAsLastGroup(groupId: String) {
        val userId = auth.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
        if (lastGroup.get(userId) == groupId) lastGroup.clear(userId)
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
        editingGroup = null,
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
