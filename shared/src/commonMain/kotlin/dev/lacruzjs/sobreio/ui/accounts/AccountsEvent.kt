package dev.lacruzjs.sobreio.ui.accounts

sealed interface AccountsEvent {
    /** The open group loaded: its currency and whether the user may edit. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canEdit: Boolean) : AccountsEvent

    /** Load the list, the figures and the members; sent each time the tab is shown. */
    data object Refresh : AccountsEvent

    /** Swipe actions of a row. Edit is handled by the Route (it opens the form); Delete archives the account at once. */
    data class EditClicked(val accountId: String) : AccountsEvent
    data class DeleteClicked(val accountId: String) : AccountsEvent

    /** Swipe action of an archived row (owner and admin): brings the account back to the list. */
    data class ReopenClicked(val accountId: String) : AccountsEvent

    data object UndoClicked : AccountsEvent
    data object UndoExpired : AccountsEvent
}
