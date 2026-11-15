package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.TagDto
import dev.lacruzjs.sobreio.data.remote.dto.TagUsageDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.TagUsage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Reads and writes the `tags` table of the group, and calls `tag_usage` and `merge_tags`. */
class SupabaseTagRepository(client: SupabaseClient) : TagRepository {

    private val postgrest = client.postgrest

    override suspend fun getTags(groupId: String): Result<List<Tag>> = safeCall {
        postgrest.from(TABLE)
            .select(Columns.raw(COLUMNS)) {
                filter {
                    eq("group_id", groupId)
                    exact("archived_at", null)
                }
                order("name", Order.ASCENDING)
            }
            .decodeList<TagDto>()
            .map { it.toDomain() }
    }

    override suspend fun getUsage(groupId: String): Result<List<TagUsage>> = safeCall {
        postgrest.rpc("tag_usage", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<TagUsageDto>()
            .map { it.toDomain() }
    }

    override suspend fun createTag(groupId: String, name: String, color: String?): Result<Tag> = safeCall {
        postgrest.from(TABLE)
            .insert(
                buildJsonObject {
                    put("group_id", groupId)
                    put("name", name)
                    if (color != null) put("color", color)
                },
            ) { select(Columns.raw(COLUMNS)) }
            .decodeSingle<TagDto>()
            .toDomain()
    }

    override suspend fun updateTag(tagId: String, name: String, color: String?): Result<Unit> = safeCall {
        update(tagId) {
            set("name", name)
            if (color != null) set("color", color) else setToNull("color")
        }
    }

    override suspend fun archiveTag(tagId: String): Result<Unit> = safeCall {
        update(tagId) { set("archived_at", Clock.System.now().toString()) }
    }

    override suspend fun unarchiveTag(tagId: String): Result<Unit> = safeCall {
        update(tagId) { setToNull("archived_at") }
    }

    override suspend fun mergeTags(sourceId: String, targetId: String): Result<Unit> = safeCall {
        postgrest.rpc(
            "merge_tags",
            buildJsonObject {
                put("p_source", sourceId)
                put("p_target", targetId)
            },
        )
        Unit
    }

    private suspend fun update(tagId: String, changes: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(changes) {
                select(Columns.list("id"))
                filter { eq("id", tagId) }
            }
            .decodeList<IdDto>()
        // Row level security hides a tag of another group from an update: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "tags"
        const val COLUMNS = "id,group_id,name,color"
    }
}
