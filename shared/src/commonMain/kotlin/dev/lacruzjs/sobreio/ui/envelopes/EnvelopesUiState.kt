package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.StringResource

const val MAX_ENVELOPE_NAME_LENGTH = 100

/** Which bottom sheet of the Envelopes tab is open. */
enum class EnvelopesSheet { None, Create, Edit, Assign }

data class EnvelopesUiState(
    /** The spending and saving envelopes, ordered by name. The "Unassigned" one is [unassigned]. */
    val envelopes: List<Envelope> = emptyList(),
    /** The system "Unassigned" envelope; null until the first load ends. */
    val unassigned: Envelope? = null,
    /** Money inside the envelopes, as the database computes it. */
    val totalCents: Long = 0,
    /** Balance of "Unassigned", as the database computes it: money still to assign. */
    val unassignedCents: Long = 0,
    /** Set by [EnvelopesEvent.GroupLoaded]; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin: they can create, edit and delete. Any member can assign. */
    val canEdit: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading. */
    val error: AppError? = null,
    val sheet: EnvelopesSheet = EnvelopesSheet.None,
    val editingEnvelope: Envelope? = null,
    val name: String = "",
    val nameError: StringResource? = null,
    val kind: EnvelopeKind = EnvelopeKind.Spending,
    /** Text of the monthly target field; blank means no target. */
    val monthlyTarget: String = "",
    val monthlyTargetError: StringResource? = null,
    /** Destination of the open Assign sheet. */
    val assignTarget: Envelope? = null,
    val amount: String = "",
    val amountError: StringResource? = null,
    /** Envelope the money comes from ("Unassigned" by default). */
    val sourceId: String? = null,
    val sourceError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of creating, editing or assigning, shown inside the open sheet. */
    val submitError: AppError? = null,
    /** Failure of deleting or undoing, shown above the list. */
    val actionError: AppError? = null,
    /** Snackbar with "Undo" for the last delete. */
    val undoMessage: UndoMessage? = null,
) {
    val spending: List<Envelope> get() = envelopes.filter { it.kind == EnvelopeKind.Spending }
    val saving: List<Envelope> get() = envelopes.filter { it.kind == EnvelopeKind.Saving }

    /** Where the money of an assignment can come from: "Unassigned" first, then the other envelopes. */
    val assignSources: List<Envelope>
        get() = (listOfNotNull(unassigned) + envelopes).filter { it.id != assignTarget?.id }
}
