package dev.lacruzjs.sobreio.ui.commitmentdetail

sealed interface CommitmentDetailEvent {
    /** Load again; sent each time the screen is shown (also when coming back from the form). */
    data object Refresh : CommitmentDetailEvent

    /** Try again after a failed load. */
    data object Retry : CommitmentDetailEvent

    /** Navigation, handled by the Route: the ViewModel ignores them. */
    data object BackClicked : CommitmentDetailEvent
    data object EditClicked : CommitmentDetailEvent
}
