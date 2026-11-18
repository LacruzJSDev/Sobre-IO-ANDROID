package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeTransferDto
import dev.lacruzjs.sobreio.data.remote.dto.LiveCommitmentDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Reads and writes `envelope_transfers` directly, like the Assign sheet; row level security lets every member do it. */
class SupabaseEnvelopeTransferRepository(client: SupabaseClient) : EnvelopeTransferRepository {

    private val postgrest = client.postgrest

    override suspend fun getTransfers(groupId: String, offset: Int, limit: Int): Result<EnvelopeTransferPage> = safeCall {
        val result = postgrest.from(TABLE)
            .select(Columns.raw(TRANSFER_COLUMNS)) {
                count(Count.EXACT)
                filter {
                    eq("group_id", groupId)
                    exact("voided_at", null)
                }
                order("created_at", Order.DESCENDING)
                order("id", Order.ASCENDING)
                range(offset.toLong(), offset.toLong() + limit - 1)
            }
        val rows = result.decodeList<EnvelopeTransferDto>()
        val names = commitmentNames(rows)
        val items = rows.map { it.toDomain(names[it.commitmentId]) }
        EnvelopeTransferPage(items, (result.countOrNull() ?: (offset + items.size).toLong()).toInt())
    }

    override suspend fun getTransfer(id: String): Result<EnvelopeTransfer> = safeCall {
        val row = postgrest.from(TABLE)
            .select(Columns.raw(TRANSFER_COLUMNS)) {
                filter {
                    eq("id", id)
                    exact("voided_at", null)
                }
            }
            .decodeSingleOrNull<EnvelopeTransferDto>()
            ?: throw AppError.NotFound
        row.toDomain(commitmentNames(listOf(row))[row.commitmentId])
    }

    override suspend fun createTransfer(groupId: String, draft: EnvelopeTransferDraft): Result<Unit> = safeCall {
        // period_id is set by a trigger and origin defaults to 'manual'
        postgrest.from(TABLE).insert(
            buildJsonObject {
                put("group_id", groupId)
                put("from_envelope_id", draft.fromEnvelopeId)
                put("to_envelope_id", draft.toEnvelopeId)
                put("amount", draft.amountCents)
                put("notes", draft.notes?.takeIf { it.isNotBlank() })
            },
        )
        Unit
    }

    override suspend fun updateTransfer(id: String, draft: EnvelopeTransferDraft): Result<Unit> = safeCall {
        val updated = postgrest.from(TABLE)
            .update({
                set("from_envelope_id", draft.fromEnvelopeId)
                set("to_envelope_id", draft.toEnvelopeId)
                set("amount", draft.amountCents)
                set("notes", draft.notes?.takeIf { it.isNotBlank() })
            }) {
                select(Columns.list("id"))
                filter {
                    eq("id", id)
                    exact("voided_at", null)
                }
            }
            .decodeList<IdDto>()
        // Row level security hides the row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun voidTransfer(id: String): Result<Unit> = safeCall {
        val updated = postgrest.from(TABLE)
            .update({ set("voided_at", Clock.System.now().toString()) }) {
                select(Columns.list("id"))
                filter {
                    eq("id", id)
                    isIn("origin", VOIDABLE_ORIGINS)
                }
            }
            .decodeList<IdDto>()
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    /** The names of the commitments the rows point to, read apart so a row never fails for the sake of its label. */
    private suspend fun commitmentNames(rows: List<EnvelopeTransferDto>): Map<String, String> {
        val ids = rows.mapNotNull { it.commitmentId }.distinct()
        if (ids.isEmpty()) return emptyMap()
        return postgrest.from(COMMITMENTS_TABLE)
            .select(Columns.list("id", "name")) { filter { isIn("id", ids) } }
            .decodeList<LiveCommitmentDto>()
            .associate { it.id to it.name }
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "envelope_transfers"
        const val COMMITMENTS_TABLE = "commitments"
    }
}
