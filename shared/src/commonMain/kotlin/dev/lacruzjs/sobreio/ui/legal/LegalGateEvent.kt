package dev.lacruzjs.sobreio.ui.legal

sealed interface LegalGateEvent {
    data class AcceptedChanged(val checked: Boolean) : LegalGateEvent
    data object ContinueClicked : LegalGateEvent
    data object SignOutClicked : LegalGateEvent

    /** Handled by the Route: opens the document in the browser. */
    data class OpenDocument(val url: String) : LegalGateEvent
}
