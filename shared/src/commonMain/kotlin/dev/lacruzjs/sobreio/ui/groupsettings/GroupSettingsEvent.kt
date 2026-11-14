package dev.lacruzjs.sobreio.ui.groupsettings

import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.ui.groups.GroupFormState

sealed interface GroupSettingsEvent {
    /** Handled by the Route, which owns the back stack. */
    data object BackClicked : GroupSettingsEvent
    data object Retry : GroupSettingsEvent
    data object NavigationHandled : GroupSettingsEvent
    data object SheetDismissed : GroupSettingsEvent

    /** Details: the "Edit" button opens the group sheet; every change of its fields comes as [FormChanged]. */
    data object EditClicked : GroupSettingsEvent
    data class FormChanged(val form: GroupFormState) : GroupSettingsEvent
    data object EditConfirmed : GroupSettingsEvent

    /** Members (owner only): swipe actions of another member's row. */
    data class RoleClicked(val userId: String) : GroupSettingsEvent
    data class RoleSelected(val role: GroupRole) : GroupSettingsEvent
    data class RemoveMemberClicked(val userId: String) : GroupSettingsEvent

    /** Invitations (owner and admin). */
    data object InviteClicked : GroupSettingsEvent
    data class InviteRoleSelected(val role: GroupRole) : GroupSettingsEvent
    data object InviteConfirmed : GroupSettingsEvent
    data class RevokeInvitationClicked(val invitationId: String) : GroupSettingsEvent

    /** The undo snackbar: "Undo" was tapped, or it timed out. */
    data object UndoClicked : GroupSettingsEvent
    data object UndoExpired : GroupSettingsEvent
}
