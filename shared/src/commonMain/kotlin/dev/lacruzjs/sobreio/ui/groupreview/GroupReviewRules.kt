package dev.lacruzjs.sobreio.ui.groupreview

import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.DueOccurrence

/**
 * What the confirm sheet says about the money set aside for the payment being registered (D-56). The envelope is
 * refilled up to what its commitments need, so a payment for less leaves money that lowers the next reserve, and a
 * payment for more takes from what the envelope already held (or is covered from Unassigned, which the cover note says).
 */
sealed interface ReserveNote {
    val reservedCents: Long

    data class Exact(override val reservedCents: Long) : ReserveNote
    data class Less(override val reservedCents: Long, val leftCents: Long) : ReserveNote
    data class More(override val reservedCents: Long, val extraCents: Long) : ReserveNote
}

/**
 * The amount the database says is set aside for this due date, or null when the commitment has no reserve for it: only
 * a funded expense (or contribution) whose next payment is this very due date and whose reserve sits in its envelope.
 * An overdue due date of an earlier cycle has no figure of its own in the status, so it shows nothing.
 */
fun reservedForDue(occurrence: DueOccurrence, funding: CommitmentFunding?): Long? {
    if (occurrence.kind != CommitmentKind.Expense && occurrence.kind != CommitmentKind.GroupTransferOut) return null
    val status = funding ?: return null
    if (status.envelopeId == null || status.envelopeId != occurrence.envelopeId) return null
    if (status.nextDueDate != occurrence.dueDate) return null
    return status.reservedInCycleCents.takeIf { it > 0 }
}

/**
 * The line for the amount being typed. With no valid amount yet it only states the reserve; a payment for more that the
 * envelope cannot cover also only states it, because the cover note right below explains the rest.
 */
fun reserveNoteOf(reservedCents: Long?, amountCents: Long?, coverShort: Boolean): ReserveNote? {
    val reserved = reservedCents ?: return null
    val amount = amountCents?.takeIf { it > 0 } ?: return ReserveNote.Exact(reserved)
    return when {
        amount < reserved -> ReserveNote.Less(reserved, reserved - amount)
        amount > reserved && !coverShort -> ReserveNote.More(reserved, amount - reserved)
        else -> ReserveNote.Exact(reserved)
    }
}
