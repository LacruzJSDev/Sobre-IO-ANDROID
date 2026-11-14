package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.MemberDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.model.Member
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

/** Reads `account_group_members` with the profile embedded (`user_id` references `profiles(id)`). */
class SupabaseMemberRepository(client: SupabaseClient) : MemberRepository {

    private val postgrest = client.postgrest

    override suspend fun getMembers(groupId: String): Result<List<Member>> = safeCall {
        postgrest.from("account_group_members")
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
}
