package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeOutlookDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopePeriodDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeTransferDto
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun EnvelopePeriodDto.toDomain() = EnvelopePeriod(
    periodId = periodId,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    openingCents = openingBalance,
    assignedCents = assigned,
    activityCents = activity,
    closingCents = closingBalance,
)

internal fun EnvelopeOutlookDto.toDomain() = EnvelopeOutlook(
    balanceCents = balance,
    committedCents = committed,
    freeCents = free,
    shortfallCents = shortfall,
)

/** [commitmentName] is the name of the commitment the move points to, read apart. */
internal fun EnvelopeTransferDto.toDomain(commitmentName: String? = null) = EnvelopeTransfer(
    id = id,
    fromEnvelopeId = fromEnvelopeId,
    toEnvelopeId = toEnvelopeId,
    amountCents = amount,
    origin = origin.toTransferOrigin(),
    createdAt = Instant.parse(createdAt),
    commitmentId = commitmentId,
    commitmentName = commitmentName,
)

/** An origin added by a newer database is treated as automatic, so it cannot be deleted by mistake. */
internal fun String?.toTransferOrigin(): TransferOrigin = when (this) {
    "manual" -> TransferOrigin.Manual
    "target" -> TransferOrigin.Target
    "commitment" -> TransferOrigin.Commitment
    "release" -> TransferOrigin.Release
    else -> TransferOrigin.Cover
}
