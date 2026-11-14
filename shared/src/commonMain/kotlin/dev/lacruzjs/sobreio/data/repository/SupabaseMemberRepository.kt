package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.MemberDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/** Reads `account_group_members` with the profile embedded (`user_id` references `profiles(id)`). */
class SupabaseMemberRepository(client: SupabaseClient) : MemberRepository {

    private val postgrest = client.postgrest

    override suspend fun getMembers(groupId: String): Result<List<Member>> = safeCall {
        postgrest.from(TABLE)
            .select(Columns.raw("user_id,role,profiles!inner(display_name)")) {
                filter {
                    eq("group_id", groupId)
                    exact("left_at", null)
                }
            }
            .decodeList<MemberDto>()
            .map { it.toDomain() }
            .sortedBy { it.displayName.lowercase() }
    }

    override suspend fun changeRole(groupId: String, userId: String, role: GroupRole): Result<Unit> = safeCall {
        updateMembership(groupId, userId) { set("role", role.toDbValue()) }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> = safeCall {
        updateMembership(groupId, userId) { set("left_at", Clock.System.now().toString()) }
    }

    private suspend fun updateMembership(groupId: String, userId: String, change: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(change) {
                select(Columns.list("user_id"))
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                    exact("left_at", null)
                }
            }
            .decodeList<UserIdDto>()
        // Row level security hides the row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class UserIdDto(val user_id: String)

    private companion object {
        const val TABLE = "account_group_members"
    }
}
