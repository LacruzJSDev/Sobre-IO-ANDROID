package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeProgressDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopesSummaryDto
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeProgress
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import kotlinx.datetime.LocalDate

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
        purpose = style?.purpose.toEnvelopePurpose(),
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

internal fun EnvelopesSummaryDto?.toSummary(): EnvelopeSummary = if (this == null) {
    EnvelopeSummary()
} else {
    EnvelopeSummary(
        unassignedCents = unassigned,
        inEnvelopesTotalCents = inEnvelopesTotal,
        dueThisPeriodCents = dueThisPeriod,
        reservedLaterCents = reservedLater,
        otherAssignedCents = otherAssigned,
        overspentCents = overspent,
        coveredOn = coveredOn?.let(LocalDate::parse),
        coveringIncomeCents = coveringIncome,
    )
}

/** Null for the system envelope and for a type added by a newer database. */
internal fun String?.toEnvelopePurpose(): EnvelopePurpose? = when (this) {
    "monthly" -> EnvelopePurpose.Monthly
    "bills" -> EnvelopePurpose.Bills
    "savings" -> EnvelopePurpose.Savings
    "free" -> EnvelopePurpose.Free
    else -> null
}

/** The value of `envelope_purpose_enum`. */
internal fun EnvelopePurpose.toDbValue(): String = when (this) {
    EnvelopePurpose.Monthly -> "monthly"
    EnvelopePurpose.Bills -> "bills"
    EnvelopePurpose.Savings -> "savings"
    EnvelopePurpose.Free -> "free"
}

/** A state this app does not know becomes [ProgressState.None]: no bar is better than a wrong one. */
internal fun String.toProgressState(): ProgressState = when (this) {
    "remaining" -> ProgressState.Remaining
    "reserving" -> ProgressState.Reserving
    "paid" -> ProgressState.Paid
    "contributing" -> ProgressState.Contributing
    "overspent" -> ProgressState.Overspent
    else -> ProgressState.None
}

internal fun EnvelopeProgressDto.toDomain() = EnvelopeProgress(
    envelopeId = envelopeId,
    purpose = purpose.toEnvelopePurpose(),
    state = state.toProgressState(),
    currentCents = current,
    goalCents = goal,
    nextDue = nextDue?.let(LocalDate::parse),
    paidOn = paidOn?.let(LocalDate::parse),
    pending = pending,
    followingDue = followingDue?.let(LocalDate::parse),
)
