package dev.lacruzjs.sobreio.ui.groupsettings

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.ui.groups.GroupFormState
import dev.lacruzjs.sobreio.ui.undo.UndoMessage

/** Which bottom sheet of the group settings is open. */
enum class GroupSettingsSheet { None, Edit, Invite, Role }

data class GroupSettingsUiState(
    val group: Group? = null,
    val currentUserId: String? = null,
    val members: List<Member> = emptyList(),
    /** Pending invitations that have not expired; only loaded for owners and admins. */
    val pendingInvitations: List<Invitation> = emptyList(),
    /** Accepted invitations: who joined and when. Read only; only loaded for owners and admins. */
    val acceptedInvitations: List<Invitation> = emptyList(),
    val isLoading: Boolean = true,
    /** Failure of loading the group. */
    val error: AppError? = null,
    val sheet: GroupSettingsSheet = GroupSettingsSheet.None,
    /** The fields of the Edit sheet. */
    val form: GroupFormState = GroupFormState(),
    val isSubmitting: Boolean = false,
    /** Failure of saving the group, creating an invitation or changing a role, shown inside the open sheet. */
    val submitError: AppError? = null,
    /** Role chosen in the Invite sheet; the invitation it creates takes it. */
    val inviteRole: GroupRole = GroupRole.Member,
    /** The invitation just created, shown with its code in the Invite sheet. */
    val createdInvitation: Invitation? = null,
    /** The member whose role the Role sheet changes. */
    val roleMember: Member? = null,
    /** Failure of an action outside the sheets (remove, revoke, undo). */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
    /** The group is gone or the user left it: the Route pops back to Groups and consumes this. */
    val navigateToGroups: Boolean = false,
) {
    /** Owner or admin of a live group: edit the details and invite. */
    val canEditDetails: Boolean get() = group?.canEdit == true
    val canInvite: Boolean get() = canEditDetails

    /** Owner of a live group: change roles and remove members. */
    val canManageMembers: Boolean get() = group?.let { it.isOwner && it.canEdit } == true

    /** More than one active member: accounts and envelopes then ask who holds them. */
    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()
}
