package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.GroupTransferDraft
import dev.lacruzjs.sobreio.domain.model.StoredTransaction
import dev.lacruzjs.sobreio.domain.model.StoredTransfer
import dev.lacruzjs.sobreio.domain.model.TransactionDraft
import dev.lacruzjs.sobreio.domain.model.TransferDraft

/**
 * What the new / edit movement form writes. Creating goes through the database functions (`create_transaction`,
 * `create_account_transfer`, `create_group_transfer`), which apply the cover and the envelope rules; editing a live row is
 * a plain update validated by the database guards and recorded in `audit_log`. Failures are
 * [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface MovementFormRepository {

    suspend fun getTransaction(id: String): Result<StoredTransaction>

    suspend fun getTransfer(id: String): Result<StoredTransfer>

    /** The ids of the tags a transaction carries. */
    suspend fun getTagIds(transactionId: String): Result<Set<String>>

    /** Returns the new transaction's id. An expense is negative in the database; an unset envelope is "Unassigned". */
    suspend fun createTransaction(draft: TransactionDraft): Result<String>

    suspend fun createTransfer(draft: TransferDraft): Result<String>

    /** Returns the id of the link between the two legs. */
    suspend fun createGroupTransfer(draft: GroupTransferDraft): Result<String>

    /** Writes account, envelope (when set), signed amount, date and notes. No cover is applied. */
    suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit>

    suspend fun updateTransfer(id: String, draft: TransferDraft): Result<Unit>

    /** Inserts the links of [added] and deletes the ones of [removed]; tags do not change any balance. */
    suspend fun syncTags(groupId: String, transactionId: String, added: Set<String>, removed: Set<String>): Result<Unit>
}
