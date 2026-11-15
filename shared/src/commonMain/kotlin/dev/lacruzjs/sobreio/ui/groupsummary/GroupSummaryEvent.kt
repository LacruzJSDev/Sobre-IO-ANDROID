package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.domain.model.GroupWarning

sealed interface GroupSummaryEvent {
    /** The open group loaded: its currency and whether the user may review due dates. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canRecord: Boolean) : GroupSummaryEvent

    /** Read everything again; sent when the group shell or the tab is shown, and by the retry button. */
    data object Refresh : GroupSummaryEvent

    /** Handled by the Route, which opens the review screen. */
    data object ReviewClicked : GroupSummaryEvent

    /** Handled by the Route, which opens the envelope, the account or the Envelopes tab. */
    data class WarningClicked(val warning: GroupWarning) : GroupSummaryEvent

    /** Handled by the Route, which opens the envelope's detail. */
    data class SpendingClicked(val envelopeId: String) : GroupSummaryEvent

    /** Handled by the Route, which selects the Envelopes tab. */
    data object SeeAllEnvelopesClicked : GroupSummaryEvent
}
