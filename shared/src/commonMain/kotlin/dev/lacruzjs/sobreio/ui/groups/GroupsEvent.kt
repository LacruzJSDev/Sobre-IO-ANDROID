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

    /** The undo snackbar: "Undo" was tapped, or it timed out. */
    data object UndoClicked : GroupsEvent
    data object UndoExpired : GroupsEvent
    data object SignOutClicked : GroupsEvent
    data object JoinClicked : GroupsEvent
    data object SheetDismissed : GroupsEvent

    data class GroupNameChanged(val value: String) : GroupsEvent
    data class CurrencySelected(val code: String) : GroupsEvent
    data object CreateConfirmed : GroupsEvent

    data class CodeChanged(val value: String) : GroupsEvent
    data object JoinConfirmed : GroupsEvent

    data object NavigationHandled : GroupsEvent
}
