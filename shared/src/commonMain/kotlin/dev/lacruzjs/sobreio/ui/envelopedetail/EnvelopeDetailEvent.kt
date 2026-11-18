package dev.lacruzjs.sobreio.ui.envelopedetail

sealed interface EnvelopeDetailEvent {
    /** Load everything; sent each time the screen is shown (also when coming back from the edit form). */
    data object Refresh : EnvelopeDetailEvent

    /** Try again after a failed load. */
    data object Retry : EnvelopeDetailEvent

    /** The moves list scrolled near its end, or its "Retry" was tapped. */
    data object LoadMoreTransfers : EnvelopeDetailEvent

    /**
     * Swipe action of a move that can be voided (manual, monthly target or reserve): it disappears at once and an Undo
     * snackbar follows.
     */
    data class DeleteTransferClicked(val transferId: String) : EnvelopeDetailEvent

    /** Navigation, handled by the Route: the ViewModel ignores them. */
    data object BackClicked : EnvelopeDetailEvent
    data object EditClicked : EnvelopeDetailEvent
    data object SeeAllClicked : EnvelopeDetailEvent
    data class CommitmentClicked(val accountId: String, val commitmentId: String) : EnvelopeDetailEvent
    /** Tap or Edit swipe of a move that can be edited: opens its form. */
    data class EditTransferClicked(val transferId: String) : EnvelopeDetailEvent

    data object UndoClicked : EnvelopeDetailEvent
    data object UndoExpired : EnvelopeDetailEvent
}
