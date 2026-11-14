package dev.lacruzjs.sobreio.ui.commitments

sealed interface CommitmentsEvent {
    /** Load the list; sent each time the tab is shown (also when coming back from the form). */
    data object Refresh : CommitmentsEvent

    /** A tap on any row (live, archived or read only). Handled by the Route, which opens the read only detail. */
    data class OpenClicked(val commitmentId: String) : CommitmentsEvent

    /** Edit swipe action. Handled by the Route, which opens the form. */
    data class EditClicked(val commitmentId: String) : CommitmentsEvent

    /** Swipe action: archives the commitment at once, with Undo. */
    data class DeleteClicked(val commitmentId: String) : CommitmentsEvent

    /** Swipe action of an archived row: brings it back, with Undo. */
    data class ReopenClicked(val commitmentId: String) : CommitmentsEvent

    data object UndoClicked : CommitmentsEvent
    data object UndoExpired : CommitmentsEvent
}
