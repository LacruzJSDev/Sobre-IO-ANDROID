package dev.lacruzjs.sobreio.data.repository

import kotlinx.serialization.SerialName
import dev.lacruzjs.sobreio.domain.model.EnvelopeTotals
import dev.lacruzjs.sobreio.domain.model.PeriodTotals
import dev.lacruzjs.sobreio.domain.model.MovementReport
import dev.lacruzjs.sobreio.domain.model.MovementTotals
import dev.lacruzjs.sobreio.data.remote.dto.MovementSearchDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodDto
import dev.lacruzjs.sobreio.data.remote.dto.PeriodRangeDto
import dev.lacruzjs.sobreio.data.remote.mapper.searchParams
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.MovementPage
import dev.lacruzjs.sobreio.domain.model.MovementSource
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
 * Reads the ledger through `search_movements` and `search_movements_count`, and voids rows with an update of
 * `voided_at`, which row level security allows to every member of the group.
 */
class SupabaseMovementRepository(client: SupabaseClient) : MovementRepository {

    private val postgrest = client.postgrest

    override suspend fun search(
        groupId: String,
        filter: MovementFilter,
        limit: Int,
        offset: Int,
        withTotal: Boolean,
    ): Result<MovementPage> = safeCall {
        val items = postgrest.rpc("search_movements", searchParams(groupId, filter, limit, offset))
            .decodeList<MovementSearchDto>()
            .map { it.toDomain() }
        val total = if (withTotal) {
            postgrest.rpc("search_movements_count", searchParams(groupId, filter)).decodeAs<Long>()
        } else {
            0L
        }
        MovementPage(items, total)
    }

    override suspend fun getTotals(groupId: String, filter: MovementFilter): Result<MovementTotals> = safeCall {
        postgrest.rpc("search_movements_summary", searchParams(groupId, filter))
            .decodeList<TotalsDto>()
            .firstOrNull()
            ?.let { MovementTotals(it.inflow, it.outflow, it.net, it.movements) }
            ?: MovementTotals(0, 0, 0, 0)
    }

    override suspend fun getReport(groupId: String, filter: MovementFilter): Result<MovementReport> = safeCall {
        val periods = postgrest.rpc("search_movements_by_period", searchParams(groupId, filter))
            .decodeList<PeriodTotalsDto>()
            .map { PeriodTotals(LocalDate.parse(it.start), LocalDate.parse(it.end), it.inflow, it.outflow, it.movements) }
        val envelopes = postgrest.rpc("search_movements_by_envelope", searchParams(groupId, filter))
            .decodeList<EnvelopeTotalsDto>()
            .map { EnvelopeTotals(it.envelopeId, it.inflow, it.outflow, it.movements) }
        MovementReport(periods, envelopes)
    }

    @Serializable
    private data class TotalsDto(val inflow: Long = 0, val outflow: Long = 0, val net: Long = 0, val movements: Long = 0)

    @Serializable
    private data class PeriodTotalsDto(
        @SerialName("period_start") val start: String,
        @SerialName("period_end") val end: String,
        val inflow: Long = 0,
        val outflow: Long = 0,
        val movements: Long = 0,
    )

    @Serializable
    private data class EnvelopeTotalsDto(
        @SerialName("envelope_id") val envelopeId: String,
        val inflow: Long = 0,
        val outflow: Long = 0,
        val movements: Long = 0,
    )

    override suspend fun voidMovement(movement: Movement): Result<Unit> = safeCall {
        val table = when (movement.source) {
            MovementSource.Transaction -> "transactions"
            MovementSource.AccountTransfer -> "account_transfers"
        }
        val updated = postgrest.from(table)
            .update({ set("voided_at", Clock.System.now().toString()) }) {
                select(Columns.list("id"))
                filter { eq("id", movement.id) }
            }
            .decodeList<IdDto>()
        // Row level security hides a row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun voidGroupTransfer(movement: Movement): Result<Unit> = safeCall {
        // The leg is a transaction; the link that pairs it is what the RPC voids. RLS shows the link only to members
        // of both groups, so an empty answer is the database saying no.
        val linkId = postgrest.from("group_transfers")
            .select(Columns.list("id")) {
                filter {
                    or {
                        eq("out_transaction_id", movement.id)
                        eq("in_transaction_id", movement.id)
                    }
                }
                limit(1)
            }
            .decodeList<IdDto>()
            .firstOrNull()
            ?.id
            ?: throw AppError.BusinessRule("FZ016")
        postgrest.rpc("void_group_transfer", buildJsonObject { put("p_link_id", linkId) })
        Unit
    }

    override suspend fun getDateContext(groupId: String): Result<DateContext> = safeCall {
        val today = LocalDate.parse(
            postgrest.rpc("group_today", buildJsonObject { put("p_group_id", groupId) }).decodeAs<String>(),
        )
        val current = postgrest.rpc("current_period", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<PeriodRangeDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound
        val previous = postgrest.from("periods")
            .select(Columns.raw("start_date,end_date")) {
                filter {
                    eq("group_id", groupId)
                    lt("start_date", current.start.toString())
                }
                order("start_date", Order.DESCENDING)
                limit(1)
            }
            .decodeList<PeriodDto>()
            .firstOrNull()
            ?.toDomain()
        DateContext(today, current, previous)
    }

    @Serializable
    private data class IdDto(val id: String)
}
