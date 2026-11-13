package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Invitation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Clock
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
    private val auth = client.auth

    override suspend fun getGroups(): Result<List<Group>> = safeCall {
        postgrest.from(TABLE)
            .select(GROUP_COLUMNS) {
                filter {
                    exact("archived_at", null)
                    exact("purge_requested_at", null)
                    mine()
                }
                order("name", Order.ASCENDING)
            }
            .decodeList<GroupDto>()
            .map { it.toDomain() }
    }

    override suspend fun getGroup(id: String): Result<Group> = safeCall {
        postgrest.from(TABLE)
            .select(GROUP_COLUMNS) {
                filter {
                    eq("id", id)
                    exact("archived_at", null)
                    exact("purge_requested_at", null)
                    mine()
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

    override suspend fun renameGroup(id: String, name: String): Result<Unit> = safeCall {
        val updated = postgrest.from(TABLE)
            .update({ set("name", name) }) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdDto>()
        // Row level security hides the group from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun requestGroupDeletion(id: String): Result<Unit> = safeCall {
        postgrest.rpc("request_group_purge", groupParam(id))
        Unit
    }

    override suspend fun cancelGroupDeletion(id: String): Result<Unit> = safeCall {
        postgrest.rpc("cancel_group_purge", groupParam(id))
        Unit
    }

    override suspend fun leaveGroup(id: String): Result<Unit> = safeCall {
        postgrest.from(MEMBERS_TABLE).update({ set("left_at", Clock.System.now().toString()) }) {
            filter {
                eq("group_id", id)
                eq("user_id", currentUserId())
            }
        }
        Unit
    }

    override suspend fun createInvitation(groupId: String): Result<Invitation> = safeCall {
        postgrest.from("invitations")
            .insert(buildJsonObject { put("group_id", groupId) }) { select(Columns.list("code", "expires_at")) }
            .decodeSingle<InvitationDto>()
            .toDomain()
    }

    private fun groupParam(id: String) = buildJsonObject { put("p_group_id", id) }

    private fun currentUserId(): String =
        auth.currentUserOrNull()?.id ?: throw AppError.Unknown(IllegalStateException("No signed-in user"))

    /**
     * Members can read their companions' rows, so the embedded membership is narrowed to the signed-in user (and
     * `!inner` drops groups the user already left).
     */
    private fun PostgrestFilterBuilder.mine() {
        eq("$MEMBERS_TABLE.user_id", currentUserId())
        exact("$MEMBERS_TABLE.left_at", null)
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "account_groups"
        const val MEMBERS_TABLE = "account_group_members"
        val GROUP_COLUMNS = Columns.raw("id,name,currency,$MEMBERS_TABLE!inner(role)")
    }
}
