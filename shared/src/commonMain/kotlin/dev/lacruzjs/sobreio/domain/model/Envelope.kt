package dev.lacruzjs.sobreio.domain.model

/**
 * What an envelope is. [System] is the "Unassigned" envelope every group has, immutable; the database also has an
 * `external` kind that the app never shows, so it is not modelled.
 */
enum class EnvelopeKind { System, Spending, Saving }

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
) {
    val isArchived: Boolean get() = archivedAt != null
}

/** What the user edits of an envelope. [ownerUserId] null is a common envelope. */
data class EnvelopeDraft(
    val name: String,
    val kind: EnvelopeKind,
    val monthlyTargetCents: Long,
    val ownerUserId: String? = null,
    val color: String? = null,
    val icon: String? = null,
)

/** The figures of the Envelopes header, as the database computes them (`group_overview`). */
data class EnvelopeSummary(
    /** Money inside the visible envelopes. */
    val totalCents: Long,
    /** Balance of the "Unassigned" envelope: money still to assign. */
    val unassignedCents: Long,
)
