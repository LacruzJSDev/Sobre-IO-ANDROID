package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import kotlinx.datetime.LocalDate

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeAccountDetailRepository : AccountDetailRepository {

    val calls = mutableListOf<String>()

    var forecast: AccountForecast? = AccountForecast(100_000, 0, 0, 100_000, 100_000, 0)
    var series = listOf(BalancePoint(LocalDate(2026, 8, 14), 50_000), BalancePoint(LocalDate(2026, 11, 14), 100_000))
    var projection = listOf(ProjectionPoint(LocalDate(2026, 11, 14), 100_000, 0))
    var flows = emptyList<PeriodFlow>()
    var upcoming = emptyList<UpcomingItem>()
    var movements = emptyList<AccountMovement>()

    var forecastResult: Result<AccountForecast?>? = null
    var seriesResult: Result<List<BalancePoint>>? = null
    var compareResult: ((Long) -> Result<BankComparison>)? = null
    var adjustmentResult: Result<String> = Result.success("tx-1")
    var voidResult: Result<Unit> = Result.success(Unit)

    override suspend fun getForecast(groupId: String, accountId: String): Result<AccountForecast?> {
        calls += "getForecast($accountId)"
        return forecastResult ?: Result.success(forecast)
    }

    var forecasts: Map<String, AccountForecast> = emptyMap()
    var balanceAt: Long = 0

    override suspend fun getBalanceAt(accountId: String, day: LocalDate): Result<Long> {
        calls += "getBalanceAt($accountId, $day)"
        return Result.success(balanceAt)
    }

    override suspend fun getForecasts(groupId: String): Result<Map<String, AccountForecast>> {
        calls += "getForecasts($groupId)"
        return Result.success(forecasts)
    }

    override suspend fun getBalanceSeries(accountId: String, from: LocalDate, to: LocalDate, points: Int): Result<List<BalancePoint>> {
        calls += "getBalanceSeries($from, $to)"
        return seriesResult ?: Result.success(series)
    }

    override suspend fun getProjection(accountId: String): Result<List<ProjectionPoint>> = Result.success(projection)

    override suspend fun getPeriodFlows(accountId: String, periods: Int): Result<List<PeriodFlow>> = Result.success(flows)

    override suspend fun getUpcoming(groupId: String, accountId: String, from: LocalDate, to: LocalDate): Result<List<UpcomingItem>> =
        Result.success(upcoming)

    override suspend fun getLatestMovements(accountId: String, count: Int): Result<List<AccountMovement>> =
        Result.success(movements)

    override suspend fun compareWithBank(accountId: String, bankCents: Long): Result<BankComparison> {
        calls += "compareWithBank($bankCents)"
        return compareResult?.invoke(bankCents) ?: Result.success(BankComparison(100_000, bankCents, bankCents - 100_000))
    }

    override suspend fun recordAdjustment(accountId: String, cents: Long): Result<String> {
        calls += "recordAdjustment($cents)"
        return adjustmentResult
    }

    override suspend fun voidTransaction(id: String): Result<Unit> {
        calls += "voidTransaction($id)"
        return voidResult
    }
}
