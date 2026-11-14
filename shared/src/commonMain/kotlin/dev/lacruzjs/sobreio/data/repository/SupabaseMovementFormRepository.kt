package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.IdRowDto
import dev.lacruzjs.sobreio.data.remote.dto.TagLinkDto
import dev.lacruzjs.sobreio.data.remote.dto.TransactionRowDto
import dev.lacruzjs.sobreio.data.remote.dto.TransferRowDto
import dev.lacruzjs.sobreio.data.remote.mapper.signed
import dev.lacruzjs.sobreio.data.remote.mapper.toCoverJson
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.GroupTransferDraft
import dev.lacruzjs.sobreio.domain.model.StoredTransaction
import dev.lacruzjs.sobreio.domain.model.StoredTransfer
import dev.lacruzjs.sobreio.domain.model.TransactionDraft
import dev.lacruzjs.sobreio.domain.model.TransferDraft
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Creates movements with the database functions and edits live rows with plain updates, which row level security allows
 * to every member of the group (the guards check the rules). Tag links are inserted and deleted in their own table.
 */
class SupabaseMovementFormRepository(client: SupabaseClient) : MovementFormRepository {

    private val postgrest = client.postgrest

    override suspend fun getTransaction(id: String): Result<StoredTransaction> = safeCall {
        postgrest.from(TRANSACTIONS)
            .select(Columns.raw("id,group_id,account_id,envelope_id,kind,amount,date,notes,voided_at")) {
                filter { eq("id", id) }
            }
            .decodeList<TransactionRowDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun getTransfer(id: String): Result<StoredTransfer> = safeCall {
        postgrest.from(TRANSFERS)
            .select(Columns.raw("id,group_id,from_account_id,to_account_id,amount,date,notes,voided_at")) {
                filter { eq("id", id) }
            }
            .decodeList<TransferRowDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun getTagIds(transactionId: String): Result<Set<String>> = safeCall {
        postgrest.from(TRANSACTION_TAGS)
            .select(Columns.list("tag_id")) { filter { eq("transaction_id", transactionId) } }
            .decodeList<TagLinkDto>()
            .map { it.tagId }
            .toSet()
    }

    override suspend fun createTransaction(draft: TransactionDraft): Result<String> = safeCall {
        postgrest.rpc(
            "create_transaction",
            buildJsonObject {
                put("p_account_id", draft.accountId)
                put("p_kind", if (draft.kind == EntryKind.Expense) "expense" else "income")
                put("p_amount", draft.kind.signed(draft.amountCents))
                put("p_envelope_id", draft.envelopeId)
                put("p_date", draft.date.toString())
                put("p_notes", draft.notes)
                draft.cover.toCoverJson()?.let { put("p_cover", it) }
            },
        ).decodeAs<String>()
    }

    override suspend fun createTransfer(draft: TransferDraft): Result<String> = safeCall {
        postgrest.rpc(
            "create_account_transfer",
            buildJsonObject {
                put("p_from_account_id", draft.fromAccountId)
                put("p_to_account_id", draft.toAccountId)
                put("p_amount", draft.amountCents)
                put("p_date", draft.date.toString())
                put("p_notes", draft.notes)
            },
        ).decodeAs<String>()
    }

    override suspend fun createGroupTransfer(draft: GroupTransferDraft): Result<String> = safeCall {
        postgrest.rpc(
            "create_group_transfer",
            buildJsonObject {
                put("p_out_account_id", draft.outAccountId)
                put("p_in_account_id", draft.inAccountId)
                put("p_amount", draft.amountCents)
                // Both envelopes are left out: "Unassigned" on each side
                put("p_out_date", draft.date.toString())
                put("p_in_date", draft.date.toString())
                put("p_notes", draft.notes)
            },
        ).decodeAs<String>()
    }

    override suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit> = safeCall {
        val updated = postgrest.from(TRANSACTIONS)
            .update(
                {
                    set("account_id", draft.accountId)
                    // Null keeps the envelope: an account outside the envelopes has a hidden one that the app never reads
                    draft.envelopeId?.let { set("envelope_id", it) }
                    set("amount", draft.kind.signed(draft.amountCents))
                    set("date", draft.date.toString())
                    set("notes", draft.notes)
                },
            ) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdRowDto>()
        // Row level security hides a row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun updateTransfer(id: String, draft: TransferDraft): Result<Unit> = safeCall {
        val updated = postgrest.from(TRANSFERS)
            .update(
                {
                    set("from_account_id", draft.fromAccountId)
                    set("to_account_id", draft.toAccountId)
                    set("amount", draft.amountCents)
                    set("date", draft.date.toString())
                    set("notes", draft.notes)
                },
            ) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdRowDto>()
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun syncTags(
        groupId: String,
        transactionId: String,
        added: Set<String>,
        removed: Set<String>,
    ): Result<Unit> = safeCall {
        if (removed.isNotEmpty()) {
            postgrest.from(TRANSACTION_TAGS).delete {
                filter {
                    eq("transaction_id", transactionId)
                    isIn("tag_id", removed.toList())
                }
            }
        }
        if (added.isNotEmpty()) {
            postgrest.from(TRANSACTION_TAGS).insert(
                buildJsonArray {
                    added.sorted().forEach { tagId ->
                        add(
                            buildJsonObject {
                                put("group_id", groupId)
                                put("transaction_id", transactionId)
                                put("tag_id", tagId)
                            },
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val TRANSACTIONS = "transactions"
        const val TRANSFERS = "account_transfers"
        const val TRANSACTION_TAGS = "transaction_tags"
    }
}
