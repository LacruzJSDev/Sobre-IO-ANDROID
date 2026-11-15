package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.DueOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.GroupOutlookDto
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
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
