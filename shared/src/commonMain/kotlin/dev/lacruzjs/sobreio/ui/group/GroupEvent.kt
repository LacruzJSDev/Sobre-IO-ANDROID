package dev.lacruzjs.sobreio.ui.group

sealed interface GroupEvent {
    data object Retry : GroupEvent

    /** Handled by the Route: the arrow goes back to Groups, like the system back. */
    data object BackClicked : GroupEvent

    /** Handled by the Route, which keeps the selected tab across recreation. */
    data class TabSelected(val tab: GroupTab) : GroupEvent

    data object NavigationHandled : GroupEvent

    /** The "+" of a tab header, and closing its sheet. */
    data object NewItemClicked : GroupEvent
    data object NewItemDismissed : GroupEvent

    /** The header's gear. Handled by the Route, which opens the group settings. */
    data object SettingsClicked : GroupEvent

    /** Reload the group quietly, for example when coming back from its settings. */
    data object Refresh : GroupEvent
}
