package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/**
 * What an envelope is. [System] is the "Unassigned" envelope every group has, immutable; the database also has an
 * `external` kind that the app never shows, so it is not modelled.
 */
enum class EnvelopeKind { System, Spending, Saving }

/**
 * The type of a live spending or saving envelope (D-46): it decides which fields the envelope has and how its progress
 * is measured. The system envelope has none. Savings envelopes are the `saving` kind, the rest `spending`.
 */
enum class EnvelopePurpose {
    /** A monthly spending envelope: its monthly target is assigned each period and must be above 0. */
    Monthly,

    /** Bills: funded commitments reserve the money of each payment here; no target. */
    Bills,

    /** Savings: an optional monthly contribution. */
    Savings,

    /** Free: only receives what is assigned by hand; no target. */
    Free,
    ;

    /** The database kind that goes with this type. */
    val kind: EnvelopeKind get() = if (this == Savings) EnvelopeKind.Saving else EnvelopeKind.Spending

    /** Whether the form asks for the monthly target and whether it is required. */
    val hasTarget: Boolean get() = this == Monthly || this == Savings
}

/**
 * An envelope of a group. [balanceCents] is computed by the database and may be negative; [monthlyTargetCents] is 0
 * when there is no target.
 */
data class Envelope(
    val id: String,
    val groupId: String,
    val name: String,
    val kind: EnvelopeKind,
    val monthlyTargetCents: Long,
    val balanceCents: Long,
    val color: String? = null,
    val icon: String? = null,
    val ownerUserId: String? = null,
    val archivedAt: String? = null,
    /** Null for the system envelope. */
    val purpose: EnvelopePurpose? = null,
) {
    val isArchived: Boolean get() = archivedAt != null
}

/** What the user edits of an envelope. [ownerUserId] null is a common envelope. */
data class EnvelopeDraft(
    val name: String,
    val purpose: EnvelopePurpose,
    /** 0 for Bills and Free: the database rejects a target there. */
    val monthlyTargetCents: Long,
    val ownerUserId: String? = null,
    val color: String? = null,
    val icon: String? = null,
) {
    /** The kind follows the type: savings are `saving`, everything else `spending`. */
    val kind: EnvelopeKind get() = purpose.kind
}

/** The figures of the Envelopes hero card, as the database computes them (`envelopes_summary`). */
data class EnvelopeSummary(
    /** Balance of the "Unassigned" envelope: money still to assign (negative when more is set aside than there is). */
    val unassignedCents: Long = 0,
    /** Money in the accounts inside the envelopes. */
    val inEnvelopesTotalCents: Long = 0,
    /** Envelope money kept for payments due up to the end of the current period. */
    val dueThisPeriodCents: Long = 0,
    /** Money in Bills envelopes set aside for payments after this period. */
    val reservedLaterCents: Long = 0,
    /** The rest of the positive envelope money. */
    val otherAssignedCents: Long = 0,
    /** Sum of the envelopes below zero, as a positive number. */
    val overspentCents: Long = 0,
    /** Only when unassigned is negative: the day the first pending income brings it back to zero or more. */
    val coveredOn: LocalDate? = null,
    /** The sum of the expected incomes into Unassigned until [coveredOn]; null together with it. */
    val coveringIncomeCents: Long? = null,
)
