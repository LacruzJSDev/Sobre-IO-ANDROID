package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import kotlinx.datetime.LocalDate

/**
 * What the account detail shows besides the account itself: forecast, charts, due items, latest movements and the bank
 * comparison. Every figure comes from the database; nothing is summed here. Failures are
 * [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface AccountDetailRepository {

    /** The group's "today" (`group_today`), in the group's time zone. */
    suspend fun getToday(groupId: String): Result<LocalDate>

    /** First and last day of the group's current period (`current_period`). */
    suspend fun getCurrentPeriod(groupId: String): Result<PeriodRange>

    /** Forecast to the end of the period of one account; null for an archived account, which the database skips. */
    suspend fun getForecast(groupId: String, accountId: String): Result<AccountForecast?>

    /** Balance over [from]..[to], at most [points] days, oldest first (`account_balance_series`). */
    suspend fun getBalanceSeries(accountId: String, from: LocalDate, to: LocalDate, points: Int = 60): Result<List<BalancePoint>>

    /** Today's balance and where it is heading until the end of the period, day by day (`account_projection`). */
    suspend fun getProjection(accountId: String): Result<List<ProjectionPoint>>

    /** The last [periods] periods, oldest first (`account_period_flows`). */
    suspend fun getPeriodFlows(accountId: String, periods: Int = 6): Result<List<PeriodFlow>>

    /**
     * Pending due items between [from] and [to] that touch the account (as source or destination), soonest first, each
     * with its amount signed for this account. The database lists the group's; keeping this account's is only a filter.
     */
    suspend fun getUpcoming(groupId: String, accountId: String, from: LocalDate, to: LocalDate): Result<List<UpcomingItem>>

    /** The newest [count] movements of the account, by date and then by when they were entered. */
    suspend fun getLatestMovements(accountId: String, count: Int = 5): Result<List<AccountMovement>>

    /** What `compare_with_bank` says for a bank balance of [bankCents]. Saves nothing. */
    suspend fun compareWithBank(accountId: String, bankCents: Long): Result<BankComparison>

    /** Records an `adjustment` transaction of [cents] (not zero) with `create_transaction`; returns the transaction id. */
    suspend fun recordAdjustment(accountId: String, cents: Long): Result<String>

    /** Voids a transaction (`voided_at`), which is how an adjustment is undone: the ledger is never deleted. */
    suspend fun voidTransaction(id: String): Result<Unit>
}
