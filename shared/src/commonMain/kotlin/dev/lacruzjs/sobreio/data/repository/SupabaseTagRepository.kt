package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.TagDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.model.Tag
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** Reads the `tags` table of the group. */
class SupabaseTagRepository(client: SupabaseClient) : TagRepository {

    private val postgrest = client.postgrest

    override suspend fun getTags(groupId: String): Result<List<Tag>> = safeCall {
        postgrest.from("tags")
            .select(Columns.raw("id,group_id,name,color")) {
                filter {
                    eq("group_id", groupId)
                    exact("archived_at", null)
                }
                order("name", Order.ASCENDING)
            }
            .decodeList<TagDto>()
            .map { it.toDomain() }
    }
}
