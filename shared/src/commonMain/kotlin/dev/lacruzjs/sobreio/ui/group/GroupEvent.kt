package dev.lacruzjs.sobreio.ui.group

sealed interface GroupEvent {
    data object Retry : GroupEvent

    /** Handled by the Route: the arrow goes back to Groups, like the system back. */
    data object MenuClicked : GroupEvent

    /** Handled by the Route, which keeps the selected tab across recreation. */
    data class TabSelected(val tab: GroupTab) : GroupEvent

    data object NavigationHandled : GroupEvent

    /** Reload the group quietly, for example when coming back from its settings. */
    data object Refresh : GroupEvent
}
