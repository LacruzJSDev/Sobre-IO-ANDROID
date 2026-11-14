package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.BalancePointDto
import dev.lacruzjs.sobreio.data.remote.dto.BankComparisonDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.MovementDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodFlowDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodRangeDto
import dev.lacruzjs.sobreio.data.remote.dto.ProjectionPointDto
import dev.lacruzjs.sobreio.data.remote.dto.UpcomingDto
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import kotlinx.datetime.LocalDate

internal fun ForecastDto.toDomain() = AccountForecast(
    balanceNowCents = balanceNow,
    scheduledInCents = scheduledIn,
    scheduledOutCents = scheduledOut,
    projectedEndCents = projectedEnd,
    projectedMinCents = projectedMin,
    shortfallCents = shortfall,
)

internal fun BalancePointDto.toDomain() = BalancePoint(LocalDate.parse(day), balance)

internal fun ProjectionPointDto.toDomain() = ProjectionPoint(LocalDate.parse(day), balance, scheduled)

internal fun PeriodFlowDto.toDomain() = PeriodFlow(
    periodId = periodId,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    inflowCents = inflow,
    outflowCents = outflow,
)

internal fun PeriodRangeDto.toDomain() = PeriodRange(LocalDate.parse(startDate), LocalDate.parse(endDate))

internal fun BankComparisonDto.toDomain() = BankComparison(appBalance, bankBalance, difference)

/**
 * The due item as it affects [accountId], or null when it does not touch it. Incomes and incoming group contributions
 * add to the account, expenses and outgoing contributions subtract, a transfer subtracts from its source and adds to its
 * destination: the same rule `forecast_events` uses in the database, only to give the amount its sign.
 */
internal fun UpcomingDto.toUpcomingFor(accountId: String): UpcomingItem? {
    val delta = when {
        kind == "transfer" && toAccountId == accountId && this.accountId != accountId -> amount
        this.accountId != accountId -> return null
        kind == "income" || kind == "group_transfer_in" -> amount
        else -> -amount
    }
    return UpcomingItem(commitmentId, name, LocalDate.parse(dueDate), delta)
}

internal fun MovementDto.toDomain() = AccountMovement(
    id = id,
    kind = kind.toMovementKind(),
    amountCents = amount,
    date = LocalDate.parse(date),
    notes = notes?.takeIf { it.isNotBlank() },
    counterAccountId = counterAccountId,
    envelopeId = envelopeId,
    createdBy = createdBy,
)

/** An unknown kind (added by a newer database) reads as an adjustment, the neutral one. */
internal fun String.toMovementKind(): MovementKind = when (this) {
    "income" -> MovementKind.Income
    "expense" -> MovementKind.Expense
    "transfer_out" -> MovementKind.TransferOut
    "transfer_in" -> MovementKind.TransferIn
    "group_transfer_out" -> MovementKind.GroupTransferOut
    "group_transfer_in" -> MovementKind.GroupTransferIn
    else -> MovementKind.Adjustment
}
