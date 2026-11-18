package dev.lacruzjs.sobreio.ui.deleteaccount

sealed interface DeleteAccountEvent {
    /** Handled by the Route, which owns the back stack. */
    data object BackClicked : DeleteAccountEvent

    /** Handled by the Route: opens that group's settings, where the roles are changed. */
    data class BlockedGroupClicked(val groupId: String) : DeleteAccountEvent

    data object Retry : DeleteAccountEvent

    /** The screen is shown again (back from a group's settings): the preview is read once more, without a spinner. */
    data object Refresh : DeleteAccountEvent

    data class ConfirmChanged(val checked: Boolean) : DeleteAccountEvent
    data object DeleteClicked : DeleteAccountEvent
}
