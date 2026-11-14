package dev.lacruzjs.sobreio.ui.groups

sealed interface GroupsEvent {
    /** Load the list; sent each time the screen is shown. */
    data object Refresh : GroupsEvent

    /** ProfileClicked and GroupClicked are handled by the Route, which owns the back stack. */
    data object ProfileClicked : GroupsEvent
    data class GroupClicked(val groupId: String) : GroupsEvent

    data object NewGroupClicked : GroupsEvent

    /** Swipe actions of a row. Delete means "request deletion" for an owner and "leave" for anyone else; no confirmation. */
    data class EditClicked(val groupId: String) : GroupsEvent
    data class DeleteClicked(val groupId: String) : GroupsEvent
    data object EditConfirmed : GroupsEvent

    /** Swipe actions of the "Archived" section (owner only): bring an archived group back, or cancel a requested deletion. */
    data class ReopenClicked(val groupId: String) : GroupsEvent
    data class CancelDeletionClicked(val groupId: String) : GroupsEvent

    /** The undo snackbar: "Undo" was tapped, or it timed out. */
    data object UndoClicked : GroupsEvent
    data object UndoExpired : GroupsEvent
    data object SignOutClicked : GroupsEvent
    data object JoinClicked : GroupsEvent
    data object SheetDismissed : GroupsEvent

    /** Any change of the sheet's fields: name, currency, colour, icon, period day or time zone. */
    data class FormChanged(val form: GroupFormState) : GroupsEvent
    data object CreateConfirmed : GroupsEvent

    data class CodeChanged(val value: String) : GroupsEvent
    data object JoinConfirmed : GroupsEvent

    data object NavigationHandled : GroupsEvent
}
