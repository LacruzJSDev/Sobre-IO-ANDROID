package dev.lacruzjs.sobreio.ui.groupsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.InvitationStatus
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.invitation_revoked
import dev.lacruzjs.sobreio.generated.resources.member_removed
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Details, members and invitations of one group. Owners and admins edit the details and invite; only owners change
 * roles (their own included) and remove other members. Archiving, deleting and leaving live in the Groups list. The
 * database enforces every rule too; this only hides what would be refused. [clock] hides invitations that expired.
 */
class GroupSettingsViewModel(
    private val groupId: String,
    private val groups: GroupRepository,
    private val members: MemberRepository,
    private val auth: AuthRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupSettingsUiState())
    val state: StateFlow<GroupSettingsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    init {
        load()
    }

    fun onEvent(event: GroupSettingsEvent) {
        when (event) {
            GroupSettingsEvent.Retry -> load()
            GroupSettingsEvent.NavigationHandled -> _state.update { it.copy(navigateToGroups = false) }
            GroupSettingsEvent.SheetDismissed ->
                _state.update { if (it.isSubmitting) it else it.copy(sheet = GroupSettingsSheet.None) }
            GroupSettingsEvent.Refresh -> if (_state.value.group != null) load()
            is GroupSettingsEvent.RoleClicked -> openRoleSheet(event.userId)
            is GroupSettingsEvent.RoleSelected -> changeRole(event.role)
            is GroupSettingsEvent.RemoveMemberClicked -> removeMember(event.userId)
            GroupSettingsEvent.InviteClicked -> openInviteSheet()
            is GroupSettingsEvent.InviteRoleSelected ->
                if (event.role != GroupRole.Owner) _state.update { it.copy(inviteRole = event.role, submitError = null) }
            GroupSettingsEvent.InviteConfirmed -> invite()
            is GroupSettingsEvent.RevokeInvitationClicked -> revokeInvitation(event.invitationId)
            GroupSettingsEvent.UndoClicked -> undo.undo()
            GroupSettingsEvent.UndoExpired -> undo.commit()
            // Handled by the Route, which owns the back stack
            GroupSettingsEvent.BackClicked, GroupSettingsEvent.EditClicked -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; an action still waiting for its snackbar has to reach the database
        undo.commitDetached()
    }

    private fun load() {
        _state.update { it.copy(isLoading = it.group == null, error = null) }
        viewModelScope.launch {
            val userId = currentUserId()
            groups.getGroup(groupId)
                .onSuccess { group ->
                    _state.update { it.copy(group = group, currentUserId = userId, isLoading = false) }
                    members.getMembers(groupId)
                        .onSuccess { list -> _state.update { it.copy(members = list) } }
                        .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
                    if (group.canEdit) {
                        groups.getInvitations(groupId)
                            .onSuccess { list ->
                                val now = clock.now()
                                _state.update {
                                    it.copy(
                                        pendingInvitations = list.filter { i ->
                                            i.status == InvitationStatus.Pending && !i.isExpired(now)
                                        },
                                        acceptedInvitations = list.filter { i -> i.status == InvitationStatus.Accepted },
                                    )
                                }
                            }
                            .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
                    } else {
                        _state.update { it.copy(pendingInvitations = emptyList(), acceptedInvitations = emptyList()) }
                    }
                }
                .onFailure { e ->
                    val error = e.asAppError()
                    if (error == AppError.NotFound) {
                        _state.update { it.copy(isLoading = false, navigateToGroups = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = error) }
                    }
                }
        }
    }

    private suspend fun currentUserId(): String =
        auth.authState.filterIsInstance<AuthState.SignedIn>().first().user.id

    // Members

    private fun otherMember(userId: String): Member? {
        val current = _state.value
        if (!current.canManageMembers || userId == current.currentUserId) return null
        return current.members.firstOrNull { it.userId == userId }
    }

    private fun openRoleSheet(userId: String) {
        val current = _state.value
        if (!current.canManageMembers) return
        // An owner may also change their own role (FZ009 refuses it when they are the last owner)
        val member = current.members.firstOrNull { it.userId == userId } ?: return
        _state.update { it.copy(sheet = GroupSettingsSheet.Role, roleMember = member, submitError = null) }
    }

    private fun changeRole(role: GroupRole) {
        val current = _state.value
        val member = current.roleMember ?: return
        if (current.isSubmitting || current.sheet != GroupSettingsSheet.Role) return
        if (member.role == role) {
            _state.update { it.copy(sheet = GroupSettingsSheet.None, roleMember = null) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            members.changeRole(groupId, member.userId, role)
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            isSubmitting = false,
                            sheet = GroupSettingsSheet.None,
                            roleMember = null,
                            members = s.members.map { if (it.userId == member.userId) it.copy(role = role) else it },
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** The member disappears at once; `left_at` is only written when the snackbar ends without "Undo". */
    private fun removeMember(userId: String) {
        val member = otherMember(userId) ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        val index = _state.value.members.indexOf(member)
        _state.update { it.copy(members = it.members - member, actionError = null) }
        undo.show(
            UndoableAction(
                message = Res.string.member_removed,
                argument = member.displayName,
                commit = { members.removeMember(groupId, member.userId) },
                undo = { Result.success(Unit) },
                restore = {
                    _state.update { it.copy(members = it.members.withInserted(index, member)) }
                },
            ),
        )
    }

    // Invitations

    private fun openInviteSheet() {
        if (!_state.value.canInvite) return
        _state.update {
            it.copy(
                sheet = GroupSettingsSheet.Invite,
                inviteRole = GroupRole.Member,
                createdInvitation = null,
                submitError = null,
            )
        }
    }

    private fun invite() {
        val current = _state.value
        if (!current.canInvite || current.isSubmitting || current.sheet != GroupSettingsSheet.Invite) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            groups.createInvitation(groupId, current.inviteRole)
                .onSuccess { invitation ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            createdInvitation = invitation,
                            pendingInvitations = listOf(invitation) + it.pendingInvitations,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** The invitation disappears at once; it is revoked in the database when the snackbar ends without "Undo". */
    private fun revokeInvitation(invitationId: String) {
        val current = _state.value
        if (!current.canInvite) return
        val invitation = current.pendingInvitations.firstOrNull { it.id == invitationId } ?: return
        undo.commit()
        val index = current.pendingInvitations.indexOf(invitation)
        _state.update { it.copy(pendingInvitations = it.pendingInvitations - invitation, actionError = null) }
        undo.show(
            UndoableAction(
                message = Res.string.invitation_revoked,
                argument = invitation.code,
                commit = { groups.revokeInvitation(invitation.id) },
                undo = { Result.success(Unit) },
                restore = {
                    _state.update { it.copy(pendingInvitations = it.pendingInvitations.withInserted(index, invitation)) }
                },
            ),
        )
    }

    private fun <T> List<T>.withInserted(index: Int, item: T): List<T> =
        toMutableList().apply { add(index.coerceIn(0, size), item) }
}
