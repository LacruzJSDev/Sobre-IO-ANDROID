package dev.lacruzjs.sobreio.ui.envelopes

sealed interface EnvelopesEvent {
    /** The open group loaded: its currency and whether the user may edit. Sent by the Route. */
    data class GroupLoaded(val currencyCode: String, val canEdit: Boolean) : EnvelopesEvent

    /** Load the summary and the list; sent each time the tab is shown. */
    data object Refresh : EnvelopesEvent

    /** Swipe actions of a row. Assign is for every member; Edit (handled by the Route, it opens the form) and Delete for owners and admins. */
    data class AssignClicked(val envelopeId: String) : EnvelopesEvent
    data class EditClicked(val envelopeId: String) : EnvelopesEvent
    data class DeleteClicked(val envelopeId: String) : EnvelopesEvent

    /** Swipe action of an archived row (owner and admin): brings the envelope back to the list. */
    data class ReopenClicked(val envelopeId: String) : EnvelopesEvent

    data object UndoClicked : EnvelopesEvent
    data object UndoExpired : EnvelopesEvent

    data object SheetDismissed : EnvelopesEvent
    data class AmountChanged(val value: String) : EnvelopesEvent
    data class SourceSelected(val envelopeId: String) : EnvelopesEvent

    /** Assign the money of the open Assign sheet. */
    data object AssignConfirmed : EnvelopesEvent
}
