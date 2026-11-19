package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.model.AuditAction
import dev.lacruzjs.sobreio.domain.model.AuditEntry
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

class SupabaseAuditRepository(client: SupabaseClient) : AuditRepository {

    private val postgrest = client.postgrest

    override suspend fun getHistory(table: String, rowId: String): Result<List<AuditEntry>> = safeCall {
        postgrest.from("audit_log")
            .select(Columns.list("action", "changed_by", "changed_at", "changes")) {
                filter {
                    eq("table_name", table)
                    eq("row_id", rowId)
                }
                order("changed_at", Order.ASCENDING)
            }
            .decodeList<AuditDto>()
            .map { dto ->
                AuditEntry(
                    action = if (dto.action == "insert") AuditAction.Created else AuditAction.Changed,
                    changedBy = dto.changedBy,
                    changedAt = Instant.parse(dto.changedAt),
                    // A change holds {column: [before, after]} for the columns that changed; a creation, the whole row
                    fields = if (dto.action == "insert") emptyList() else dto.changes.keys.sorted(),
                )
            }
    }

    @Serializable
    private data class AuditDto(
        val action: String,
        @SerialName("changed_by") val changedBy: String? = null,
        @SerialName("changed_at") val changedAt: String,
        val changes: JsonObject = JsonObject(emptyMap()),
    )
}
