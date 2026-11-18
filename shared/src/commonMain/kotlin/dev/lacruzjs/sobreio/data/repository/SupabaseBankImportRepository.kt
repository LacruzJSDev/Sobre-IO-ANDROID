package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.BankImportOverviewDto
import dev.lacruzjs.sobreio.data.remote.dto.BankRowPreviewDto
import dev.lacruzjs.sobreio.data.remote.mapper.choiceRowsJson
import dev.lacruzjs.sobreio.data.remote.mapper.previewRowsJson
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.BankImportSummary
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** [BankImportRepository] on `bank_import_preview`, `apply_bank_import`, `void_bank_import` and `bank_import_overview`. */
class SupabaseBankImportRepository(client: SupabaseClient) : BankImportRepository {

    private val postgrest = client.postgrest

    override suspend fun preview(accountId: String, rows: List<StatementRow>): Result<List<BankRowPreview>> = safeCall {
        postgrest.rpc(
            "bank_import_preview",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_rows", previewRowsJson(rows))
            },
        ).decodeList<BankRowPreviewDto>().map { it.toDomain() }.sortedBy { it.index }
    }

    override suspend fun apply(
        accountId: String,
        fileName: String,
        tagName: String,
        tagColor: String,
        choices: List<BankRowChoice>,
    ): Result<BankImportSummary> = safeCall {
        val importId = postgrest.rpc(
            "apply_bank_import",
            buildJsonObject {
                put("p_account_id", accountId)
                put("p_file_name", fileName)
                put("p_tag_name", tagName)
                put("p_tag_color", tagColor)
                put("p_rows", choiceRowsJson(choices))
            },
        ).decodeAs<String>()
        readOverview(importId) ?: throw AppError.NotFound
    }.recoverAlreadyImported()

    override suspend fun undo(importId: String): Result<Unit> = safeCall {
        postgrest.rpc("void_bank_import", buildJsonObject { put("p_import_id", importId) })
        Unit
    }

    override suspend fun getLastImport(accountId: String): Result<BankImportSummary?> = safeCall {
        postgrest.from("bank_import_overview").select {
            filter {
                eq("account_id", accountId)
                exact("voided_at", null)
            }
            order("created_at", Order.DESCENDING)
            limit(1)
        }.decodeList<BankImportOverviewDto>().firstOrNull()?.toDomain()
    }

    private suspend fun readOverview(importId: String): BankImportSummary? =
        postgrest.from("bank_import_overview").select { filter { eq("id", importId) } }
            .decodeList<BankImportOverviewDto>().firstOrNull()?.toDomain()

    /** 23505 here is never a name clash: a statement reference was imported meanwhile (D-52). */
    private fun <T> Result<T>.recoverAlreadyImported(): Result<T> =
        if (exceptionOrNull() == AppError.DuplicateName) Result.failure(AppError.BusinessRule(BANK_ROW_ALREADY_IMPORTED)) else this
}
