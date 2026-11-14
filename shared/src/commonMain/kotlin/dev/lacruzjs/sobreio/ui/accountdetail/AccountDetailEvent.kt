package dev.lacruzjs.sobreio.ui.accountdetail

sealed interface AccountDetailEvent {
    /** Load everything; sent each time the screen is shown (also when coming back from the edit form). */
    data object Refresh : AccountDetailEvent

    /** Try again after a failed load. */
    data object Retry : AccountDetailEvent

    /** The tab is kept by the Route (it survives rotation), like the group's tabs. */
    data class TabSelected(val tab: AccountDetailTab) : AccountDetailEvent

    data class RangeSelected(val range: BalanceRange) : AccountDetailEvent

    /** Navigation, handled by the Route: the ViewModel ignores them. */
    data object BackClicked : AccountDetailEvent
    data object EditClicked : AccountDetailEvent
    data object SeeAllClicked : AccountDetailEvent

    data object CompareClicked : AccountDetailEvent
    data object CompareDismissed : AccountDetailEvent
    data class BankBalanceChanged(val value: String) : AccountDetailEvent
    data object AdjustClicked : AccountDetailEvent

    data object UndoClicked : AccountDetailEvent
    data object UndoExpired : AccountDetailEvent
}
