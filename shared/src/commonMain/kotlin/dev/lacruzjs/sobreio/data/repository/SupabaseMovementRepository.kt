package dev.lacruzjs.sobreio.data.repository

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
