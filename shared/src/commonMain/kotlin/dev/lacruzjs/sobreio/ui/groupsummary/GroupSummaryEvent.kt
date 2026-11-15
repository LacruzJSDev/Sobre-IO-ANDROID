package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.domain.model.GroupWarning
import kotlinx.datetime.LocalDate

sealed interface GroupSummaryEvent {
    /** The open group loaded: its currency and whether the user may register movements. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canRecord: Boolean) : GroupSummaryEvent

    /** Read everything again; sent when the group shell or the tab is shown, and by the retry button. */
    data object Refresh : GroupSummaryEvent

    /** [key] is [dev.lacruzjs.sobreio.domain.model.DueOccurrence.key]. */
    data class ConfirmClicked(val key: String) : GroupSummaryEvent
    data class SkipClicked(val key: String) : GroupSummaryEvent

    data class AmountChanged(val value: String) : GroupSummaryEvent
    data class DateChanged(val date: LocalDate) : GroupSummaryEvent
    data class CoverSelected(val envelopeId: String?) : GroupSummaryEvent
    data object ConfirmSubmitted : GroupSummaryEvent
    data object SheetDismissed : GroupSummaryEvent

    /** Handled by the Route, which opens the envelope, the account or the Envelopes tab. */
    data class WarningClicked(val warning: GroupWarning) : GroupSummaryEvent

    data object UndoClicked : GroupSummaryEvent
    data object UndoExpired : GroupSummaryEvent
}
