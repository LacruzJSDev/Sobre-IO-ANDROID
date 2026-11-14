package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.StringResource

/** Which bottom sheet of the Envelopes tab is open. */
enum class EnvelopesSheet { None, Assign }

data class EnvelopesUiState(
    /** The live spending and saving envelopes, ordered by name. The "Unassigned" one is [unassigned]. */
    val envelopes: List<Envelope> = emptyList(),
    /** The archived envelopes, ordered by name: the collapsed "Archived" section at the end of the list. */
    val archivedEnvelopes: List<Envelope> = emptyList(),
    /** The active members: holder names are shown only when there are several. */
    val members: List<Member> = emptyList(),
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
    /** Destination of the open Assign sheet. */
    val assignTarget: Envelope? = null,
    val amount: String = "",
    val amountError: StringResource? = null,
    /** Envelope the money comes from ("Unassigned" by default). */
    val sourceId: String? = null,
    val sourceError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of assigning, shown inside the open sheet. */
    val submitError: AppError? = null,
    /** Failure of deleting, reopening or undoing, shown above the list. */
    val actionError: AppError? = null,
    /** Snackbar for the last delete or reopen. */
    val undoMessage: UndoMessage? = null,
) {
    val spending: List<Envelope> get() = envelopes.filter { it.kind == EnvelopeKind.Spending }
    val saving: List<Envelope> get() = envelopes.filter { it.kind == EnvelopeKind.Saving }

    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()

    /** Name of the holder of [envelope], or null for a common envelope or when the group has one holder only. */
    fun holderName(envelope: Envelope): String? {
        if (!hasSeveralMembers) return null
        return members.firstOrNull { it.userId == envelope.ownerUserId }?.displayName
    }

    /** Where the money of an assignment can come from: "Unassigned" first, then the other envelopes. */
    val assignSources: List<Envelope>
        get() = (listOfNotNull(unassigned) + envelopes).filter { it.id != assignTarget?.id }
}
