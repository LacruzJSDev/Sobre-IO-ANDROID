package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.DueOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeSpendingDto
import dev.lacruzjs.sobreio.data.remote.dto.FreePointDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.GroupOutlookDto
import dev.lacruzjs.sobreio.data.remote.dto.TransferPlanDto
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.EnvelopeSpending
import dev.lacruzjs.sobreio.domain.model.FreePoint
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import kotlinx.datetime.LocalDate

internal fun GroupOutlookDto.toDomain() = GroupOutlook(
    periodStart = LocalDate.parse(periodStart),
    periodEnd = LocalDate.parse(periodEnd),
    unassignedCents = unassigned,
    overspentCents = overspent,
    freeTodayCents = freeToday,
    committedShortfallCents = committedShortfall,
    pendingOutCents = pendingOut,
    pendingIncomeCents = pendingIncome,
    freeProjectedCents = freeProjected,
    pendingTargetsCents = pendingTargets,
)

internal fun DueOccurrenceDto.toDomain() = DueOccurrence(
    commitmentId = commitmentId,
    versionId = versionId,
    name = name,
    kind = kind.toCommitmentKind(),
    dueDate = LocalDate.parse(dueDate),
    amountCents = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    envelopeId = envelopeId,
    autoRegister = autoRegister,
)

/** Only the accounts that would run short: the database says 0 for a credit card and for accounts that stay positive. */
internal fun List<ForecastDto>.toShortfalls(): List<AccountShortfall> =
    filter { it.shortfall > 0 }.map { AccountShortfall(it.accountId, it.shortfall, it.projectedMin) }

internal fun FreePointDto.toDomain() = FreePoint(day = LocalDate.parse(day), freeCents = free)

/** `activity` is negative for spending; the bar shows the amount spent, positive. */
internal fun EnvelopeSpendingDto.toDomain() = EnvelopeSpending(envelopeId = envelopeId, spentCents = -activity)

internal fun TransferPlanDto.toDomain() = TransferSuggestion(
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amountCents = amount,
    neededBy = LocalDate.parse(neededBy),
    crossesBoundary = crossesBoundary,
)
