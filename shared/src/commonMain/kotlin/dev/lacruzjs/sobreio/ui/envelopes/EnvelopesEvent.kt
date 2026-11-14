package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.model.EnvelopeKind

sealed interface EnvelopesEvent {
    /** The open group loaded: its currency and whether the user may edit. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canEdit: Boolean) : EnvelopesEvent

    /** Load the summary and the list; sent each time the tab is shown. */
    data object Refresh : EnvelopesEvent

    /** The "+" of the header. Ignored for members. */
    data object NewEnvelopeClicked : EnvelopesEvent

    /** Swipe actions of a row. Assign is for every member; Edit and Delete for owners and admins. */
    data class AssignClicked(val envelopeId: String) : EnvelopesEvent
    data class EditClicked(val envelopeId: String) : EnvelopesEvent
    data class DeleteClicked(val envelopeId: String) : EnvelopesEvent

    data object UndoClicked : EnvelopesEvent
    data object UndoExpired : EnvelopesEvent

    data object SheetDismissed : EnvelopesEvent
    data class NameChanged(val value: String) : EnvelopesEvent
    data class KindSelected(val kind: EnvelopeKind) : EnvelopesEvent
    data class MonthlyTargetChanged(val value: String) : EnvelopesEvent
    data class AmountChanged(val value: String) : EnvelopesEvent
    data class SourceSelected(val envelopeId: String) : EnvelopesEvent

    /** Create or save, depending on the open form sheet. */
    data object ConfirmClicked : EnvelopesEvent

    /** Assign the money of the open Assign sheet. */
    data object AssignConfirmed : EnvelopesEvent
}
