package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.CommitmentFundingDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentIdDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentLinkDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentWithVersionsDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentChange
import dev.lacruzjs.sobreio.domain.model.CommitmentDraft
import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.OccurrencePage
import dev.lacruzjs.sobreio.domain.model.LinkedCommitmentDraft
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * Reads `commitments` with their `commitment_versions` embedded and writes through the `create_commitment`,
 * `change_commitment` and linked-commitment RPCs, which hold every rule (versions, shapes, dates). Archiving and renaming
 * are plain updates that row level security limits to owners and admins.
 */
class SupabaseCommitmentRepository(client: SupabaseClient) : CommitmentRepository {

    private val postgrest = client.postgrest

    override suspend fun getAccountCommitments(accountId: String): Result<List<Commitment>> = safeCall {
        val ids = postgrest.from(VERSIONS_TABLE)
            .select(Columns.list("commitment_id")) {
                filter {
                    or {
                        eq("account_id", accountId)
                        eq("to_account_id", accountId)
                    }
                }
            }
            .decodeList<CommitmentIdDto>()
            .map { it.commitmentId }
            .distinct()
        if (ids.isEmpty()) return@safeCall emptyList()
        postgrest.from(TABLE)
            .select(Columns.raw(COLUMNS_WITH_VERSIONS)) { filter { isIn("id", ids) } }
            .decodeList<CommitmentWithVersionsDto>()
            .map { it.header().toDomain(it.versions) }
    }

    override suspend fun getCommitment(id: String): Result<Commitment> = safeCall {
        postgrest.from(TABLE)
            .select(Columns.raw(COLUMNS_WITH_VERSIONS)) { filter { eq("id", id) } }
            .decodeSingleOrNull<CommitmentWithVersionsDto>()
            ?.let { it.header().toDomain(it.versions) }
            ?: throw AppError.NotFound
    }

    override suspend fun getFundingStatus(commitmentId: String): Result<CommitmentFunding?> = safeCall {
        postgrest.rpc("commitment_funding_status", buildJsonObject { put("p_commitment_id", commitmentId) })
            .decodeList<CommitmentFundingDto>()
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun getOccurrences(commitmentId: String, offset: Int, limit: Int): Result<OccurrencePage> = safeCall {
        val result = postgrest.from(OCCURRENCES_TABLE)
            .select(Columns.raw(OCCURRENCE_COLUMNS)) {
                count(Count.EXACT)
                filter { eq("commitment_id", commitmentId) }
                order("due_date", Order.DESCENDING)
                range(offset.toLong(), offset.toLong() + limit - 1)
            }
        val items = result.decodeList<CommitmentOccurrenceDto>().map { it.toDomain() }
        OccurrencePage(items, (result.countOrNull() ?: (offset + items.size).toLong()).toInt())
    }

    override suspend fun getLinks(commitmentIds: List<String>): Result<List<CommitmentLink>> = safeCall {
        if (commitmentIds.isEmpty()) return@safeCall emptyList()
        postgrest.from(LINKS_TABLE)
            .select(Columns.raw("id,out_group_id,in_group_id,out_commitment_id,in_commitment_id")) {
                filter {
                    or {
                        isIn("out_commitment_id", commitmentIds)
                        isIn("in_commitment_id", commitmentIds)
                    }
                }
            }
            .decodeList<CommitmentLinkDto>()
            .map { it.toDomain() }
    }

    override suspend fun createCommitment(draft: CommitmentDraft): Result<String> = safeCall {
        postgrest.rpc(
            "create_commitment",
            buildJsonObject {
                put("p_name", draft.name)
                put("p_kind", draft.kind.toDbValue())
                put("p_amount", draft.amountCents)
                put("p_account_id", draft.accountId)
                put("p_first_due_date", draft.firstDueDate.toString())
                put("p_to_account_id", draft.toAccountId)
                put("p_envelope_id", draft.envelopeId)
                put("p_is_recurring", draft.isRecurring)
                put("p_every_n", draft.everyN)
                put("p_unit", draft.unit.toDbValue())
                put("p_end_date", draft.endDate?.toString())
                put("p_funded", draft.funded)
                put("p_auto_register", draft.autoRegister)
                put("p_notes", draft.notes)
            },
        ).decodeAs<String>()
    }

    override suspend fun changeCommitment(id: String, change: CommitmentChange): Result<Unit> = safeCall {
        postgrest.rpc(
            "change_commitment",
            buildJsonObject {
                put("p_commitment_id", id)
                put("p_amount", change.amountCents)
                put("p_account_id", change.accountId)
                put("p_first_due_date", change.firstDueDate.toString())
                put("p_to_account_id", change.toAccountId)
                put("p_envelope_id", change.envelopeId)
                put("p_is_recurring", change.isRecurring)
                put("p_every_n", change.everyN)
                put("p_unit", change.unit.toDbValue())
                put("p_end_date", change.endDate?.toString())
                put("p_funded", change.funded)
                put("p_auto_register", change.autoRegister)
                put("p_notes", change.notes)
                put("p_effective_from", change.effectiveFrom?.toString())
            },
        )
        Unit
    }

    override suspend fun renameCommitment(id: String, name: String): Result<Unit> = safeCall {
        update(listOf(id)) { set("name", name) }
    }

    override suspend fun archiveCommitment(id: String): Result<Unit> = safeCall {
        update(listOf(id)) { set("archived_at", Clock.System.now().toString()) }
    }

    override suspend fun unarchiveCommitment(id: String): Result<Unit> = safeCall {
        update(listOf(id)) { setToNull("archived_at") }
    }

    override suspend fun createLinkedCommitment(draft: LinkedCommitmentDraft): Result<String> = safeCall {
        postgrest.rpc(
            "create_linked_commitment",
            buildJsonObject {
                put("p_name", draft.name)
                put("p_out_account_id", draft.outAccountId)
                put("p_in_account_id", draft.inAccountId)
                put("p_amount", draft.amountCents)
                put("p_first_due_date", draft.firstDueDate.toString())
                put("p_is_recurring", draft.isRecurring)
                put("p_every_n", draft.everyN)
                put("p_unit", draft.unit.toDbValue())
                put("p_end_date", draft.endDate?.toString())
                put("p_out_envelope_id", draft.outEnvelopeId)
                put("p_in_envelope_id", draft.inEnvelopeId)
                put("p_funded", draft.funded)
                put("p_auto_register", draft.autoRegister)
            },
        ).decodeAs<String>()
    }

    override suspend fun changeLinkedCommitment(linkId: String, amountCents: Long, effectiveFrom: LocalDate?): Result<Unit> =
        safeCall {
            postgrest.rpc(
                "change_linked_commitment",
                buildJsonObject {
                    put("p_link_id", linkId)
                    put("p_amount", amountCents)
                    put("p_effective_from", effectiveFrom?.toString())
                },
            )
            Unit
        }

    override suspend fun archiveLinkedCommitment(linkId: String): Result<Unit> = safeCall {
        postgrest.rpc("archive_linked_commitment", buildJsonObject { put("p_link_id", linkId) })
        Unit
    }

    override suspend fun unarchiveLinkedCommitment(link: CommitmentLink): Result<Unit> = safeCall {
        update(listOf(link.outCommitmentId, link.inCommitmentId)) { setToNull("archived_at") }
    }

    private suspend fun update(ids: List<String>, changes: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(changes) {
                select(Columns.list("id"))
                filter { isIn("id", ids) }
            }
            .decodeList<IdDto>()
        // Row level security hides a commitment from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "commitments"
        const val VERSIONS_TABLE = "commitment_versions"
        const val LINKS_TABLE = "commitment_links"
        const val OCCURRENCES_TABLE = "commitment_occurrences"
        const val OCCURRENCE_COLUMNS = "due_date,status,transaction_id,transfer_id,resolved_at," +
            "transactions(amount,date),account_transfers(amount,date,from_account_id)"
        const val COLUMNS_WITH_VERSIONS = "id,group_id,name,notes,archived_at," +
            "commitment_versions(id,commitment_id,effective_from,effective_to,kind,amount,account_id,to_account_id," +
            "envelope_id,first_due_date,is_recurring,every_n,unit,anchor_day,end_date,funded,auto_register,notes)"
    }
}
