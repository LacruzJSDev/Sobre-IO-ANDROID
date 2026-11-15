package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.MovementFormRepository
import dev.lacruzjs.sobreio.domain.model.GroupTransferDraft
import dev.lacruzjs.sobreio.domain.model.StoredTransaction
import dev.lacruzjs.sobreio.domain.model.StoredTransfer
import dev.lacruzjs.sobreio.domain.model.TransactionDraft
import dev.lacruzjs.sobreio.domain.model.TransferDraft

/** Calls are recorded in [calls] and the last drafts kept; set the `*Result` fields to script failures. */
class FakeMovementFormRepository : MovementFormRepository {

    val calls = mutableListOf<String>()

    var transaction: StoredTransaction? = null
    var transfer: StoredTransfer? = null
    var tagIds: Set<String> = emptySet()

    var createdTransaction: TransactionDraft? = null
    var updatedTransaction: TransactionDraft? = null
    var createdTransfer: TransferDraft? = null
    var updatedTransfer: TransferDraft? = null
    var createdGroupTransfer: GroupTransferDraft? = null
    var syncedAdded: Set<String>? = null
    var syncedRemoved: Set<String>? = null

    var createResult: Result<String> = Result.success("new-tx")
    var updateResult: Result<Unit> = Result.success(Unit)
    var syncResult: Result<Unit> = Result.success(Unit)

    override suspend fun getTransaction(id: String): Result<StoredTransaction> {
        calls += "getTransaction($id)"
        return transaction?.let { Result.success(it) } ?: Result.failure(dev.lacruzjs.sobreio.domain.error.AppError.NotFound)
    }

    override suspend fun getTransfer(id: String): Result<StoredTransfer> {
        calls += "getTransfer($id)"
        return transfer?.let { Result.success(it) } ?: Result.failure(dev.lacruzjs.sobreio.domain.error.AppError.NotFound)
    }

    override suspend fun getTagIds(transactionId: String): Result<Set<String>> = Result.success(tagIds)

    override suspend fun createTransaction(draft: TransactionDraft): Result<String> {
        calls += "createTransaction"
        createdTransaction = draft
        return createResult
    }

    override suspend fun createTransfer(draft: TransferDraft): Result<String> {
        calls += "createTransfer"
        createdTransfer = draft
        return createResult
    }

    override suspend fun createGroupTransfer(draft: GroupTransferDraft): Result<String> {
        calls += "createGroupTransfer"
        createdGroupTransfer = draft
        return createResult
    }

    override suspend fun updateTransaction(id: String, draft: TransactionDraft): Result<Unit> {
        calls += "updateTransaction($id)"
        updatedTransaction = draft
        return updateResult
    }

    override suspend fun updateTransfer(id: String, draft: TransferDraft): Result<Unit> {
        calls += "updateTransfer($id)"
        updatedTransfer = draft
        return updateResult
    }

    override suspend fun syncTags(groupId: String, transactionId: String, added: Set<String>, removed: Set<String>): Result<Unit> {
        calls += "syncTags($transactionId)"
        syncedAdded = added
        syncedRemoved = removed
        return syncResult
    }
}
