package dev.lacruzjs.sobreio.ui.groupreview

import kotlinx.datetime.LocalDate

sealed interface GroupReviewEvent {
    /** Read the group and its due dates again; sent when the screen is shown, and by the retry button. */
    data object Refresh : GroupReviewEvent

    /** Handled by the Route, which goes back to the group. */
    data object BackClicked : GroupReviewEvent

    /** [key] is [dev.lacruzjs.sobreio.domain.model.DueOccurrence.key]. */
    data class ConfirmClicked(val key: String) : GroupReviewEvent
    data class SkipClicked(val key: String) : GroupReviewEvent

    data class AmountChanged(val value: String) : GroupReviewEvent
    data class DateChanged(val date: LocalDate) : GroupReviewEvent
    data class CoverSelected(val envelopeId: String?) : GroupReviewEvent
    data object ConfirmSubmitted : GroupReviewEvent
    data object SheetDismissed : GroupReviewEvent

    data object UndoClicked : GroupReviewEvent
    data object UndoExpired : GroupReviewEvent
}
