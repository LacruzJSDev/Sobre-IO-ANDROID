package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.model.AccountKind

sealed interface AccountsEvent {
    /** The open group loaded: its currency and whether the user may edit. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canEdit: Boolean) : AccountsEvent

    /** Load the list and the total; sent each time the tab is shown. */
    data object Refresh : AccountsEvent

    /** The "+" of the header. Ignored for members. */
    data object NewAccountClicked : AccountsEvent

    /** Swipe actions of a row. Delete archives the account at once, without confirmation. */
    data class EditClicked(val accountId: String) : AccountsEvent
    data class DeleteClicked(val accountId: String) : AccountsEvent

    data object UndoClicked : AccountsEvent
    data object UndoExpired : AccountsEvent

    data object SheetDismissed : AccountsEvent
    data class NameChanged(val value: String) : AccountsEvent
    data class KindSelected(val kind: AccountKind) : AccountsEvent
    data class OpeningBalanceChanged(val value: String) : AccountsEvent

    /** Create or save, depending on the open sheet. */
    data object ConfirmClicked : AccountsEvent
}
