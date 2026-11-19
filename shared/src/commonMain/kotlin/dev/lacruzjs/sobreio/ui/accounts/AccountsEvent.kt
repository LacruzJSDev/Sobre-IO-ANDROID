package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.model.TransferSuggestion

sealed interface AccountsEvent {
    /** The open group loaded: its currency and whether the user may edit. Sent by the Route. */
    /** [canRecord]: any member of a live group may record the transfers the plan suggests. */
    data class GroupLoaded(val currencyCode: String, val canEdit: Boolean, val canRecord: Boolean = false) : AccountsEvent

    /** "Transfer" next to a suggestion of the plan: the Route opens the movement form already filled. */
    data class TransferClicked(val suggestion: TransferSuggestion) : AccountsEvent

    /** Load the list, the figures and the members; sent each time the tab is shown. */
    data object Refresh : AccountsEvent

    /** Swipe actions of a row. Edit is handled by the Route (it opens the form); Delete archives the account at once. */
    data class EditClicked(val accountId: String) : AccountsEvent

    /** A tap on a row: the Route opens the account's detail. */
    data class OpenClicked(val accountId: String) : AccountsEvent
    data class DeleteClicked(val accountId: String) : AccountsEvent

    /** Swipe action of an archived row (owner and admin): brings the account back to the list. */
    data class ReopenClicked(val accountId: String) : AccountsEvent

    data object UndoClicked : AccountsEvent
    data object UndoExpired : AccountsEvent
}
