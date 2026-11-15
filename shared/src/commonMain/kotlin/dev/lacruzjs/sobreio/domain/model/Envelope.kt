package dev.lacruzjs.sobreio.domain.model

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

/** The figures of the Envelopes header, as the database computes them (`group_overview`). */
data class EnvelopeSummary(
    /** Money inside the visible envelopes. */
    val totalCents: Long,
    /** Balance of the "Unassigned" envelope: money still to assign. */
    val unassignedCents: Long,
)
