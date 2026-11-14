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
            GroupsEvent.EditConfirmed -> saveEdit()
            is GroupsEvent.DeleteClicked -> deleteOrLeave(event.groupId)
            GroupsEvent.UndoClicked -> undo.undo()
            GroupsEvent.UndoExpired -> undo.commit()
            GroupsEvent.SignOutClicked -> signOut()
            GroupsEvent.JoinClicked -> _state.update { it.openSheet(GroupsSheet.Join) }
            GroupsEvent.SheetDismissed -> _state.update { if (it.isSubmitting) it else it.copy(sheet = GroupsSheet.None) }
            is GroupsEvent.FormChanged -> _state.update { it.copy(form = event.form, submitError = null) }
            is GroupsEvent.ReopenClicked -> reopen(event.groupId)
            is GroupsEvent.CancelDeletionClicked -> cancelDeletion(event.groupId)
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
                .onSuccess { list -> _state.update { it.withGroups(list) } }
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
        _state.update { it.openSheet(GroupsSheet.Edit).copy(form = GroupFormState.forEdit(group)) }
    }

    private fun saveEdit() {
        val current = _state.value
        val group = current.form.editing ?: return
        if (current.isSubmitting) return
        val form = current.form.validated()
        if (form.nameError != null) {
            _state.update { it.copy(form = form) }
            return
        }
        val name = form.name.trim()
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.updateGroup(group.id, name, form.timezone, form.periodStartDay, form.color, form.icon)
                .onSuccess {
                    _state.update { s ->
                        val updated = group.copy(
                            name = name,
                            color = form.color,
                            icon = form.icon,
                            timezone = form.timezone,
                            periodStartDay = form.periodStartDay,
                        )
                        s.copy(
                            isSubmitting = false,
                            sheet = GroupsSheet.None,
                            groups = s.groups.map { if (it.id == group.id) updated else it }.sortedByName(),
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** Owner of an archived group: it becomes live again and moves to the main list. */
    private fun reopen(groupId: String) {
        val group = _state.value.archivedGroups.firstOrNull { it.id == groupId && it.isArchived && it.isOwner } ?: return
        _state.update { it.copy(actionError = null) }
        viewModelScope.launch {
            groups.reopenGroup(group.id)
                .onSuccess { load() }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
        }
    }

    /** Owner of a group pending deletion: the request is cancelled and the group is live (or archived) again. */
    private fun cancelDeletion(groupId: String) {
        val group = _state.value.archivedGroups.firstOrNull { it.id == groupId && it.isPendingDeletion && it.isOwner } ?: return
        _state.update { it.copy(actionError = null) }
        viewModelScope.launch {
            groups.cancelGroupDeletion(group.id)
                .onSuccess { load() }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
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
        val restore = { _state.update { it.copy(groups = (it.groups + group).sortedByName()) } }

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
        val form = current.form.validated()
        if (form.nameError != null) {
            _state.update { it.copy(form = form) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.createGroup(
                name = form.name.trim(),
                currencyCode = form.currency,
                timezone = form.timezone,
                periodStartDay = form.periodStartDay,
                color = form.color,
                icon = form.icon,
            )
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

    private fun GroupsUiState.openSheet(sheet: GroupsSheet) = copy(
        sheet = sheet,
        form = GroupFormState.forNew(),
        code = "",
        codeError = null,
        submitError = null,
    )

    private fun GroupsUiState.opened(groupId: String) =
        copy(isSubmitting = false, sheet = GroupsSheet.None, navigateToGroup = groupId)

    /** Live groups in the main list; archived ones and the ones pending deletion in the "Archived" section. */
    private fun GroupsUiState.withGroups(all: List<Group>): GroupsUiState {
        val (live, hidden) = all.partition { !it.isArchived && !it.isPendingDeletion }
        val archived = hidden.filter { !it.isPendingDeletion }.sortedByName()
        val pending = hidden.filter { it.isPendingDeletion }.sortedByName()
        return copy(groups = live.sortedByName(), archivedGroups = archived + pending, isLoading = false, error = null)
    }

    private fun List<Group>.sortedByName() = sortedBy { it.name.lowercase() }
}
