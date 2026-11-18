package dev.lacruzjs.sobreio.ui.envelopedetail

sealed interface EnvelopeDetailEvent {
    /** Load everything; sent each time the screen is shown (also when coming back from the edit form). */
    data object Refresh : EnvelopeDetailEvent

    /** Try again after a failed load. */
    data object Retry : EnvelopeDetailEvent

    /** Navigation, handled by the Route: the ViewModel ignores them. */
    data object BackClicked : EnvelopeDetailEvent
    data object EditClicked : EnvelopeDetailEvent
    /** The icon of the latest movements section: every movement of the envelope. */
    data object SeeAllClicked : EnvelopeDetailEvent
    /** The icon of the moves section: the envelope's moves between envelopes, where they are edited and deleted. */
    data object TransfersClicked : EnvelopeDetailEvent
    data class CommitmentClicked(val accountId: String, val commitmentId: String) : EnvelopeDetailEvent
}
