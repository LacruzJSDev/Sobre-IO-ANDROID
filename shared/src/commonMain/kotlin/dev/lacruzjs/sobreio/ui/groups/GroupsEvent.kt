package dev.lacruzjs.sobreio.ui.groups

sealed interface GroupsEvent {
    /** Load the list; sent each time the screen is shown. */
    data object Refresh : GroupsEvent

    /** The Route handles these, it owns the back stack: profile, a group, and the "+" that opens the group form. */
    data object ProfileClicked : GroupsEvent
    data object NewGroupClicked : GroupsEvent
    data class GroupClicked(val groupId: String) : GroupsEvent


    /**
     * Swipe actions of a row, none confirmed: Archive and Delete (owner only) and Leave (everyone, owners included).
     * Delete requests the purge at once; Leave is deferred until the snackbar ends.
     */
    data class ArchiveClicked(val groupId: String) : GroupsEvent
    data class DeleteClicked(val groupId: String) : GroupsEvent
    data class LeaveClicked(val groupId: String) : GroupsEvent

    /** Swipe actions of the "Archived" section (owner only): bring an archived group back, or cancel a requested deletion. */
    data class ReopenClicked(val groupId: String) : GroupsEvent
    data class CancelDeletionClicked(val groupId: String) : GroupsEvent

    /** The undo snackbar: "Undo" was tapped, or it timed out. */
    data object UndoClicked : GroupsEvent
    data object UndoExpired : GroupsEvent
    data object SignOutClicked : GroupsEvent
    data object JoinClicked : GroupsEvent
    data object SheetDismissed : GroupsEvent

    data class CodeChanged(val value: String) : GroupsEvent
    data object JoinConfirmed : GroupsEvent

    data object NavigationHandled : GroupsEvent
}
