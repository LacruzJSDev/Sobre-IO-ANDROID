package dev.lacruzjs.sobreio.ui.groups

sealed interface GroupsEvent {
    /** Load the list; sent each time the screen is shown. */
    data object Refresh : GroupsEvent

    /** The next three are handled by the Route, which owns the back stack. */
    data object ProfileClicked : GroupsEvent
    data class GroupClicked(val groupId: String) : GroupsEvent

    data object NewGroupClicked : GroupsEvent
    data object JoinClicked : GroupsEvent
    data object SheetDismissed : GroupsEvent

    data class GroupNameChanged(val value: String) : GroupsEvent
    data class CurrencySelected(val code: String) : GroupsEvent
    data object CreateConfirmed : GroupsEvent

    data class CodeChanged(val value: String) : GroupsEvent
    data object JoinConfirmed : GroupsEvent

    data object NavigationHandled : GroupsEvent
}
