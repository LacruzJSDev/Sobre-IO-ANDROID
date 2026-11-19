package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/**
 * What `account_forecast` says about one account until the end of the current period, in cents, as the database computes
 * it. [shortfallCents] is how much is missing to stay at or above zero; it is always 0 for a credit card.
 */
data class AccountForecast(
    val balanceNowCents: Long,
    val scheduledInCents: Long,
    val scheduledOutCents: Long,
    val projectedEndCents: Long,
    val projectedMinCents: Long,
    val shortfallCents: Long,
)

/** One point of `account_balance_series`: the balance at the end of [day]. */
data class BalancePoint(val day: LocalDate, val balanceCents: Long)

/** One row of `account_projection`: the balance after the pending items of [day]; [scheduledCents] is the day's net. */
data class ProjectionPoint(val day: LocalDate, val balanceCents: Long, val scheduledCents: Long)

/** Money in and out of an account during one period (`account_period_flows`). [outflowCents] is positive. */
data class PeriodFlow(
    val periodId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val inflowCents: Long,
    val outflowCents: Long,
)

/** A pending due item that touches the account. [amountCents] is signed for this account (negative leaves it). */
data class UpcomingItem(
    val commitmentId: String,
    val name: String,
    val dueDate: LocalDate,
    val amountCents: Long,
)

/** The kinds a row of the `movements` view can have. */
enum class MovementKind { Income, Expense, Adjustment, TransferOut, TransferIn, GroupTransferOut, GroupTransferIn }

/**
 * A row of the `movements` view seen from one account: [amountCents] is signed for it. [counterAccountId] is the other
 * account of an account transfer; [envelopeId] is null for transfers. [createdBy] is the user who entered it.
 */
data class AccountMovement(
    val id: String,
    val kind: MovementKind,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
    val counterAccountId: String?,
    val envelopeId: String?,
    val createdBy: String?,
)

/** Answer of `compare_with_bank`: [differenceCents] is the bank balance minus the app's, what would be left to record. */
data class BankComparison(val appBalanceCents: Long, val bankBalanceCents: Long, val differenceCents: Long)

/**
 * The day on which the balance reaches the forecast's lowest point: the first projection row at [minCents] or, when the
 * database's within-day ordering put the minimum between two rows, the first row with the lowest balance. Null without rows.
 */
fun shortfallDate(projection: List<ProjectionPoint>, minCents: Long): LocalDate? {
    val exact = projection.firstOrNull { it.balanceCents == minCents }
    if (exact != null) return exact.day
    val lowest = projection.minOfOrNull { it.balanceCents } ?: return null
    return projection.first { it.balanceCents == lowest }.day
}
