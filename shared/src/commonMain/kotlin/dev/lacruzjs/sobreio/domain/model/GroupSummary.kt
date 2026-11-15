package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/**
 * The group's money outlook for the current period (`group_outlook_current`), as the database computes it. [freeTodayCents]
 * is what is really free today; [freeProjectedCents] what will be free at [periodEnd]; [committedShortfallCents] is what the
 * envelopes lack for the period's commitments (0 when they cover them).
 */
data class GroupOutlook(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val unassignedCents: Long,
    val overspentCents: Long,
    val freeTodayCents: Long,
    val committedShortfallCents: Long,
    val pendingOutCents: Long,
    val pendingIncomeCents: Long,
    val freeProjectedCents: Long,
)

/** One day of `group_free_projection`: what is really free at the end of [day] (the group's outlook up to that day). */
data class FreePoint(val day: LocalDate, val freeCents: Long)

/** What an envelope spent in the current period: the negative `activity` of its `envelope_periods` row, as a positive amount. */
data class EnvelopeSpending(val envelopeId: String, val spentCents: Long)

/**
 * One due date of a commitment (a row of `pending_occurrences` or `upcoming_occurrences`). [amountCents] is the expected
 * amount, always positive; [signedCents] gives it the sign of the source account's side.
 */
data class DueOccurrence(
    val commitmentId: String,
    val versionId: String,
    val name: String,
    val kind: CommitmentKind,
    val dueDate: LocalDate,
    val amountCents: Long,
    val accountId: String?,
    val toAccountId: String?,
    val envelopeId: String?,
    val autoRegister: Boolean,
) {
    /** Income and incoming contributions enter the account; everything else leaves it. */
    val signedCents: Long
        get() = if (kind == CommitmentKind.Income || kind == CommitmentKind.GroupTransferIn) amountCents else -amountCents

    /** The receiving half of a contribution is registered by whoever contributes, never from this group. */
    val isReviewable: Boolean get() = kind != CommitmentKind.GroupTransferIn

    /** Identifies the due date inside the group. */
    val key: String get() = "$commitmentId|$dueDate"
}

/** An account whose forecast goes below zero before the end of the period (`account_forecast.shortfall > 0`). */
data class AccountShortfall(val accountId: String, val shortfallCents: Long, val lowestCents: Long)

/** What `confirm_occurrence` created: [id] is a transaction, an account transfer or a group transfer link, by [kind]. */
data class ConfirmedFact(val id: String, val kind: CommitmentKind)

/**
 * A row of `transfer_plan`: move [amountCents] from [fromAccountId] to [toAccountId] before [neededBy], so the receiver
 * does not go below zero. A null [fromAccountId] is the part nobody can cover. [crossesBoundary] means the two accounts are
 * on different sides of the envelope boundary.
 */
data class TransferSuggestion(
    val fromAccountId: String?,
    val toAccountId: String,
    val amountCents: Long,
    val neededBy: LocalDate,
    val crossesBoundary: Boolean,
)

/** Which way a transfer that crosses the envelope boundary moves the money. */
enum class BoundaryCrossing {
    /** From an account outside the envelopes into one inside them: the money enters the distribution. */
    Enters,

    /** From an account inside the envelopes to one outside them: the money leaves the distribution. */
    Leaves,
}

/** A suggestion of the plan as a warning line shows it: with the source account's name and the crossing direction. */
data class TransferHint(
    val suggestion: TransferSuggestion,
    val fromName: String?,
    val crossing: BoundaryCrossing?,
)

/** A line of the Group tab's warnings card; tapping it opens [target]. */
sealed interface GroupWarning {
    data class EnvelopeOverspent(val envelopeId: String, val name: String, val overspentCents: Long) : GroupWarning
    data class AccountShortfallOn(
        val accountId: String,
        val name: String,
        val lowestCents: Long,
        val date: LocalDate,
        /** What `transfer_plan` suggests to cover this account, one line each; empty when it has no suggestion. */
        val hints: List<TransferHint> = emptyList(),
    ) : GroupWarning
    data class CommitmentsShortfall(val cents: Long) : GroupWarning
}
