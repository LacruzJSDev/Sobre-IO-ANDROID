package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AccountDeletionRepository
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem

class FakeAccountDeletionRepository : AccountDeletionRepository {

    var preview: Result<List<DeletionPreviewItem>> = Result.success(emptyList())
    var deleteResult: Result<Unit> = Result.success(Unit)
    var previewCalls = 0
    var deleteCalls = 0

    override suspend fun getPreview(): Result<List<DeletionPreviewItem>> {
        previewCalls++
        return preview
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteCalls++
        return deleteResult
    }
}
