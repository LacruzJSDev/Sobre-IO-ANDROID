package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Row level security returns only the groups where the user is an active member, so no membership filter is needed. */
class SupabaseGroupRepository(client: SupabaseClient) : GroupRepository {

    private val postgrest = client.postgrest
    private val auth = client.auth

    override suspend fun getGroups(): Result<List<Group>> = safeCall {
        postgrest.from(TABLE)
            .select(GROUP_COLUMNS) {
                filter { mine() }
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
                    exact("purge_requested_at", null)
                    mine()
                }
            }
            .decodeSingleOrNull<GroupDto>()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun createGroup(
        name: String,
        currencyCode: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<String> = safeCall {
        postgrest.rpc(
            "create_group",
            buildJsonObject {
                put("p_name", name)
                put("p_currency", currencyCode)
                put("p_timezone", timezone)
                put("p_period_start_day", periodStartDay)
                put("p_color", color)
                put("p_icon", icon)
            },
        ).decodeAs<String>()
    }

    override suspend fun joinGroup(code: String): Result<String> = safeCall {
        postgrest.rpc("accept_invitation", buildJsonObject { put("p_code", code) }).decodeAs<String>()
    }

    override suspend fun updateGroup(
        id: String,
        name: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<Unit> = safeCall {
        updateGroupRow(id) {
            set("name", name)
            set("timezone", timezone)
            set("period_start_day", periodStartDay)
            set("color", color)
            set("icon", icon)
        }
    }

    override suspend fun archiveGroup(id: String): Result<Unit> = safeCall {
        updateGroupRow(id) { set("archived_at", Clock.System.now().toString()) }
    }

    override suspend fun reopenGroup(id: String): Result<Unit> = safeCall {
        updateGroupRow(id) { setToNull("archived_at") }
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

    override suspend fun createInvitation(groupId: String, role: GroupRole): Result<Invitation> = safeCall {
        postgrest.from(INVITATIONS_TABLE)
            .insert(
                buildJsonObject {
                    put("group_id", groupId)
                    put("role", role.toDbValue())
                },
            ) { select(INVITATION_COLUMNS) }
            .decodeSingle<InvitationDto>()
            .toDomain()
    }

    override suspend fun getPendingInvitations(groupId: String): Result<List<Invitation>> = safeCall {
        postgrest.from(INVITATIONS_TABLE)
            .select(INVITATION_COLUMNS) {
                filter {
                    eq("group_id", groupId)
                    eq("status", "pending")
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<InvitationDto>()
            .map { it.toDomain() }
    }

    override suspend fun revokeInvitation(id: String): Result<Unit> = safeCall {
        val updated = postgrest.from(INVITATIONS_TABLE)
            .update({ set("status", "revoked") }) {
                select(Columns.list("id"))
                filter {
                    eq("id", id)
                    eq("status", "pending")
                }
            }
            .decodeList<IdDto>()
        // Row level security hides the invitation from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    /** Updates one `account_groups` row; an update the policy refuses returns no row instead of an error. */
    private suspend fun updateGroupRow(id: String, change: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(change) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdDto>()
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
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
        const val INVITATIONS_TABLE = "invitations"
        val GROUP_COLUMNS = Columns.raw(
            "id,name,currency,timezone,period_start_day,color,icon,archived_at,purge_requested_at," +
                "$MEMBERS_TABLE!inner(role)",
        )
        val INVITATION_COLUMNS = Columns.list("id", "code", "role", "expires_at")
    }
}
