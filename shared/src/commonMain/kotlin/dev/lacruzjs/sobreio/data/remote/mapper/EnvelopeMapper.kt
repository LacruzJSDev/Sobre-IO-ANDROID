package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopesOverviewDto
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary

/** Null for an envelope the app never shows (`external`, or a kind added by a newer database). */
internal fun EnvelopeDto.toDomain(style: EnvelopeStyleDto? = null): Envelope? {
    val kind = kind.toEnvelopeKind() ?: return null
    return Envelope(
        id = id,
        groupId = groupId,
        name = name,
        kind = kind,
        monthlyTargetCents = monthlyTarget,
        balanceCents = balance,
        color = style?.color,
        icon = style?.icon,
        ownerUserId = ownerUserId,
        archivedAt = archivedAt,
    )
}

internal fun String?.toEnvelopeKind(): EnvelopeKind? = when (this) {
    "system" -> EnvelopeKind.System
    "spending" -> EnvelopeKind.Spending
    "saving" -> EnvelopeKind.Saving
    else -> null
}

/** The value of `envelope_kind_enum`. */
internal fun EnvelopeKind.toDbValue(): String = when (this) {
    EnvelopeKind.System -> "system"
    EnvelopeKind.Spending -> "spending"
    EnvelopeKind.Saving -> "saving"
}

internal fun EnvelopesOverviewDto?.toSummary(): EnvelopeSummary = EnvelopeSummary(
    totalCents = this?.inEnvelopesTotal ?: 0L,
    unassignedCents = this?.unassignedBalance ?: 0L,
)
