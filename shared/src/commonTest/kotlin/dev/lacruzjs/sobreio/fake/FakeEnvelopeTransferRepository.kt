package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.EnvelopeTransferRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeEnvelopeTransferRepository(var transfers: List<EnvelopeTransfer> = emptyList()) : EnvelopeTransferRepository {

    val calls = mutableListOf<String>()
    val voided = mutableListOf<String>()
    var createdDraft: EnvelopeTransferDraft? = null
    var updatedDraft: EnvelopeTransferDraft? = null

    var pageResult: Result<EnvelopeTransferPage>? = null
    var createResult: Result<Unit> = Result.success(Unit)
    var updateResult: Result<Unit> = Result.success(Unit)
    var voidResult: Result<Unit> = Result.success(Unit)

    override suspend fun getTransfers(groupId: String, envelopeId: String?, offset: Int, limit: Int): Result<EnvelopeTransferPage> {
        calls += if (envelopeId == null) "getTransfers($groupId, $offset, $limit)" else "getTransfers($groupId, $envelopeId, $offset, $limit)"
        val rows = if (envelopeId == null) transfers else transfers.filter { it.fromEnvelopeId == envelopeId || it.toEnvelopeId == envelopeId }
        return pageResult ?: Result.success(EnvelopeTransferPage(rows.drop(offset).take(limit), rows.size))
    }

    override suspend fun getTransfer(id: String): Result<EnvelopeTransfer> {
        calls += "getTransfer($id)"
        return transfers.firstOrNull { it.id == id }?.let { Result.success(it) } ?: Result.failure(AppError.NotFound)
    }

    override suspend fun createTransfer(groupId: String, draft: EnvelopeTransferDraft): Result<Unit> {
        calls += "createTransfer($groupId, ${draft.fromEnvelopeId}, ${draft.toEnvelopeId}, ${draft.amountCents})"
        createdDraft = draft
        return createResult
    }

    override suspend fun updateTransfer(id: String, draft: EnvelopeTransferDraft): Result<Unit> {
        calls += "updateTransfer($id, ${draft.fromEnvelopeId}, ${draft.toEnvelopeId}, ${draft.amountCents})"
        updatedDraft = draft
        return updateResult
    }

    override suspend fun voidTransfer(id: String): Result<Unit> {
        calls += "voidTransfer($id)"
        return voidResult.onSuccess { voided += id }
    }
}
