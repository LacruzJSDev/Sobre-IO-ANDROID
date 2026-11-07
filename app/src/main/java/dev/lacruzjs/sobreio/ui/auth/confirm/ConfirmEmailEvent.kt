package dev.lacruzjs.sobreio.ui.auth.confirm

sealed interface ConfirmEmailEvent {
    data class CodeChanged(val value: String) : ConfirmEmailEvent
    data object ConfirmClicked : ConfirmEmailEvent
    data object ResendClicked : ConfirmEmailEvent
    data object BackToSignInClicked : ConfirmEmailEvent
}
