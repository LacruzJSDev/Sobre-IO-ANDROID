package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * One period of an envelope's life (`envelope_periods`), as the database computes it: [openingCents] is what it carried
 * in, [assignedCents] the net money assigned (received minus sent), [activityCents] the transactions dated inside the
 * period (negative = spending) and [closingCents] the balance at its end. Nothing here is summed by the app.
 */
data class EnvelopePeriod(
    val periodId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val openingCents: Long,
    val assignedCents: Long,
    val activityCents: Long,
    val closingCents: Long,
) {
    /** In [day]'s period. */
    fun contains(day: LocalDate): Boolean = day in startDate..endDate
}

/**
 * What an envelope can really spend until the end of the period (`envelope_outlook_current`): [committedCents] is what
 * its commitments will take, [freeCents] the balance after that and [shortfallCents] how much of the commitments the
 * balance does not cover (0 when it does).
 */
data class EnvelopeOutlook(
    val balanceCents: Long,
    val committedCents: Long,
    val freeCents: Long,
    val shortfallCents: Long,
)

/** Where a move between envelopes came from (`envelope_transfer_origin_enum`). Only [Manual] ones can be deleted. */
enum class TransferOrigin { Manual, Target, Commitment, Cover }

/**
 * A live row of `envelope_transfers`: [amountCents] is positive and moves money from [fromEnvelopeId] to [toEnvelopeId].
 * [commitmentId] and [commitmentName] are set for [TransferOrigin.Commitment]. [createdAt] is when it was entered.
 */
data class EnvelopeTransfer(
    val id: String,
    val fromEnvelopeId: String,
    val toEnvelopeId: String,
    val amountCents: Long,
    val origin: TransferOrigin,
    val createdAt: Instant,
    val commitmentId: String? = null,
    val commitmentName: String? = null,
)

/** A page of moves, newest first, and the exact number of live moves the envelope has in all. */
data class EnvelopeTransferPage(val items: List<EnvelopeTransfer>, val total: Int)

/**
 * A commitment that may fund an envelope: [accountId] is the account of its newest funded version, to open the commitment
 * detail as that account sees it. Whether it funds the envelope in this period is decided by `commitment_funding_status`.
 */
data class FundingCommitment(
    val commitmentId: String,
    val name: String,
    val accountId: String,
    val kind: CommitmentKind,
)
