package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.BankImportRepository
import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.model.BankImportSummary
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import dev.lacruzjs.sobreio.domain.model.BankRowStatus

/** Answers every preview row with [defaultPreview] unless [previews] has one for its index. */
class FakeBankImportRepository(
    var lastImport: BankImportSummary? = null,
    var previews: Map<Int, BankRowPreview> = emptyMap(),
) : BankImportRepository {
    var previewResult: Result<List<BankRowPreview>>? = null
    var applyResult: Result<BankImportSummary>? = null
    var undoResult: Result<Unit> = Result.success(Unit)
    val previewed = mutableListOf<List<StatementRow>>()
    val applied = mutableListOf<List<BankRowChoice>>()
    val appliedTags = mutableListOf<String>()
    val undone = mutableListOf<String>()

    override suspend fun preview(accountId: String, rows: List<StatementRow>): Result<List<BankRowPreview>> {
        previewed += rows
        return previewResult ?: Result.success(rows.map { previews[it.index] ?: defaultPreview(it.index) })
    }

    override suspend fun apply(
        accountId: String,
        fileName: String,
        tagName: String,
        tagColor: String,
        choices: List<BankRowChoice>,
    ): Result<BankImportSummary> {
        applied += choices
        appliedTags += tagName
        return applyResult ?: Result.success(
            summary(
                accountId,
                created = choices.count { it.action == BankRowAction.Create },
                confirmed = choices.count { it.action == BankRowAction.Confirm },
                linked = choices.count { it.action == BankRowAction.Link },
                ignored = choices.count { it.action == BankRowAction.Ignore },
            ),
        )
    }

    override suspend fun undo(importId: String): Result<Unit> {
        undone += importId
        return undoResult
    }

    override suspend fun getLastImport(accountId: String): Result<BankImportSummary?> = Result.success(lastImport)

    companion object {
        fun defaultPreview(index: Int) = BankRowPreview(
            index = index, status = BankRowStatus.New, suggestedAction = BankRowAction.Create,
            commitmentId = null, dueDate = null, matchSource = null, matchId = null, envelopeId = null,
            pending = emptyList(), existing = emptyList(),
        )

        fun summary(accountId: String, created: Int = 0, confirmed: Int = 0, linked: Int = 0, ignored: Int = 0) = BankImportSummary(
            id = "imp1", accountId = accountId, fileName = "s.csv", tagId = "tag1", createdDay = null,
            rowsTotal = created + confirmed + linked + ignored, created = created, confirmed = confirmed, linked = linked,
            ignored = ignored, firstDate = null, lastDate = null, isVoided = false,
        )
    }
}
