package dev.lacruzjs.sobreio.ui.group

sealed interface GroupEvent {
    data object Retry : GroupEvent

    /** Handled by the Route: the arrow goes back to Groups, like the system back. */
    data object BackClicked : GroupEvent

    /** Handled by the Route, which keeps the selected tab across recreation. */
    data class TabSelected(val tab: GroupTab) : GroupEvent

    data object NavigationHandled : GroupEvent
}
