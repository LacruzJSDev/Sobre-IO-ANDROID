package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.BalancePointDto
import dev.lacruzjs.sobreio.data.remote.dto.BankComparisonDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.MovementDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodFlowDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodRangeDto
import dev.lacruzjs.sobreio.data.remote.dto.ProjectionPointDto
import dev.lacruzjs.sobreio.data.remote.dto.UpcomingDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.mapper.toUpcomingFor
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * Reads the account detail through the read functions and views of the database. Writes only the adjustment (the
 * `create_transaction` RPC) and its void, which row level security allows to every member.
 */
class SupabaseAccountDetailRepository(client: SupabaseClient) : AccountDetailRepository {

    private val postgrest = client.postgrest

    override suspend fun getToday(groupId: String): Result<LocalDate> = safeCall {
        LocalDate.parse(
            postgrest.rpc("group_today", buildJsonObject { put("p_group_id", groupId) }).decodeAs<String>(),
        )
    }

    override suspend fun getCurrentPeriod(groupId: String): Result<PeriodRange> = safeCall {
        postgrest.rpc("current_period", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<PeriodRangeDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun getForecast(groupId: String, accountId: String): Result<AccountForecast?> = safeCall {
        postgrest.rpc("account_forecast", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<ForecastDto>()
            .firstOrNull { it.accountId == accountId }
            ?.toDomain()
    }

    override suspend fun getBalanceSeries(
        accountId: String,
        from: LocalDate,
        to: LocalDate,
        points: Int,
    ): Result<List<BalancePoint>> = safeCall {
        postgrest.rpc(
            "account_balance_series",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_from", from.toString())
                put("p_to", to.toString())
                put("p_points", points)
            },
        ).decodeList<BalancePointDto>().map { it.toDomain() }
    }

    override suspend fun getProjection(accountId: String): Result<List<ProjectionPoint>> = safeCall {
        postgrest.rpc("account_projection", buildJsonObject { put("p_account_id", accountId) })
            .decodeList<ProjectionPointDto>()
            .map { it.toDomain() }
    }

    override suspend fun getPeriodFlows(accountId: String, periods: Int): Result<List<PeriodFlow>> = safeCall {
        postgrest.rpc(
            "account_period_flows",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_periods", periods)
            },
        ).decodeList<PeriodFlowDto>().map { it.toDomain() }
    }

    override suspend fun getUpcoming(
        groupId: String,
        accountId: String,
        from: LocalDate,
        to: LocalDate,
    ): Result<List<UpcomingItem>> = safeCall {
        postgrest.rpc(
            "upcoming_occurrences",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_from", from.toString())
                put("p_to", to.toString())
            },
        ).decodeList<UpcomingDto>().mapNotNull { it.toUpcomingFor(accountId) }
    }

    override suspend fun getLatestMovements(accountId: String, count: Int): Result<List<AccountMovement>> = safeCall {
        postgrest.from(MOVEMENTS_VIEW)
            .select(Columns.raw("id,kind,amount,date,notes,counter_account_id,envelope_id,created_by")) {
                filter { eq("account_id", accountId) }
                order("date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
                limit(count.toLong())
            }
            .decodeList<MovementDto>()
            .map { it.toDomain() }
    }

    override suspend fun compareWithBank(accountId: String, bankCents: Long): Result<BankComparison> = safeCall {
        postgrest.rpc(
            "compare_with_bank",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_bank_balance", bankCents)
            },
        ).decodeList<BankComparisonDto>().firstOrNull()?.toDomain() ?: throw AppError.NotFound
    }

    override suspend fun recordAdjustment(accountId: String, cents: Long): Result<String> = safeCall {
        postgrest.rpc(
            "create_transaction",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_kind", "adjustment")
                put("p_amount", cents)
            },
        ).decodeAs<String>()
    }

    override suspend fun voidTransaction(id: String): Result<Unit> = safeCall {
        val updated = postgrest.from(TRANSACTIONS_TABLE)
            .update({ set("voided_at", Clock.System.now().toString()) }) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdDto>()
        // Row level security hides the row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val MOVEMENTS_VIEW = "movements"
        const val TRANSACTIONS_TABLE = "transactions"
    }
}
