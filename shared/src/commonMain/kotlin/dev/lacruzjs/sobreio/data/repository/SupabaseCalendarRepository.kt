package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.PeriodRangeDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.GroupPeriod
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseCalendarRepository(client: SupabaseClient) : CalendarRepository {

    private val postgrest = client.postgrest

    override suspend fun getToday(groupId: String): Result<LocalDate> = safeCall { today(groupId) }

    override suspend fun getCurrentPeriod(groupId: String): Result<PeriodRange> = safeCall { currentPeriod(groupId) }

    override suspend fun getDateContext(groupId: String): Result<DateContext> = safeCall {
        val current = currentPeriod(groupId)
        val previous = postgrest.from(PERIODS)
            .select(Columns.raw("start_date,end_date")) {
                filter {
                    eq("group_id", groupId)
                    lt("start_date", current.start.toString())
                }
                order("start_date", Order.DESCENDING)
                limit(1)
            }
            .decodeList<PeriodRangeDto>()
            .firstOrNull()
            ?.toDomain()
        DateContext(today(groupId), current, previous)
    }

    override suspend fun getPeriods(groupId: String, limit: Int): Result<List<GroupPeriod>> = safeCall {
        // Periods are created as the calendar reaches them (D-08): there is none in the future
        postgrest.from(PERIODS)
            .select(Columns.raw("id,start_date,end_date")) {
                filter { eq("group_id", groupId) }
                order("start_date", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<PeriodRowDto>()
            .map { GroupPeriod(it.id, LocalDate.parse(it.startDate), LocalDate.parse(it.endDate)) }
    }

    private suspend fun today(groupId: String): LocalDate = LocalDate.parse(
        postgrest.rpc("group_today", buildJsonObject { put("p_group_id", groupId) }).decodeAs<String>(),
    )

    private suspend fun currentPeriod(groupId: String): PeriodRange =
        postgrest.rpc("current_period", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<PeriodRangeDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound

    @Serializable
    private data class PeriodRowDto(
        val id: String,
        @SerialName("start_date") val startDate: String,
        @SerialName("end_date") val endDate: String,
    )

    private companion object {
        const val PERIODS = "periods"
    }
}
