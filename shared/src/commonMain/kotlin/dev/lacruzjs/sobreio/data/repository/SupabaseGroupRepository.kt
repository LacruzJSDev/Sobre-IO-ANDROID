package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Row level security returns only the groups where the user is an active member, so no membership filter is needed.
 * [timeZoneId] is the device time zone, the one a new group starts with.
 */
class SupabaseGroupRepository(
    client: SupabaseClient,
    private val timeZoneId: () -> String = { TimeZone.currentSystemDefault().id },
) : GroupRepository {

    private val postgrest = client.postgrest

    override suspend fun getGroups(): Result<List<Group>> = safeCall {
        postgrest.from(TABLE)
            .select(Columns.list("id", "name", "currency")) {
                filter { exact("archived_at", null) }
                order("name", Order.ASCENDING)
            }
            .decodeList<GroupDto>()
            .map { it.toDomain() }
    }

    override suspend fun getGroup(id: String): Result<Group> = safeCall {
        postgrest.from(TABLE)
            .select(Columns.list("id", "name", "currency")) {
                filter {
                    eq("id", id)
                    exact("archived_at", null)
                }
            }
            .decodeSingleOrNull<GroupDto>()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun createGroup(name: String, currencyCode: String): Result<String> = safeCall {
        postgrest.rpc(
            "create_group",
            buildJsonObject {
                put("p_name", name)
                put("p_currency", currencyCode)
                put("p_timezone", timeZoneId())
            },
        ).decodeAs<String>()
    }

    override suspend fun joinGroup(code: String): Result<String> = safeCall {
        postgrest.rpc("accept_invitation", buildJsonObject { put("p_code", code) }).decodeAs<String>()
    }

    private companion object {
        const val TABLE = "account_groups"
    }
}
