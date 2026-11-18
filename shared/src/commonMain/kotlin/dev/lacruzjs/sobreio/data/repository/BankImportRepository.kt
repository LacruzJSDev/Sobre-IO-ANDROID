package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.model.BankImportSummary
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview

/**
 * Bank statement imports of an account (D-52). The app reads the CSV; the database says what each row is,
 * records every decision in one call and undoes an import as a whole.
 */
interface BankImportRepository {

    /** What each row is and what to do with it; writes nothing. One result per row, in the rows' order. */
    suspend fun preview(accountId: String, rows: List<StatementRow>): Result<List<BankRowPreview>>

    /**
     * Records the statement and tags its transactions with [tagName] (created with [tagColor] when missing).
     * Returns the import. A row imported meanwhile fails the whole call with [AlreadyImported][dev.lacruzjs.sobreio.domain.error.AppError.BusinessRule].
     */
    suspend fun apply(
        accountId: String,
        fileName: String,
        tagName: String,
        tagColor: String,
        choices: List<BankRowChoice>,
    ): Result<BankImportSummary>

    /** Undoes an import: voids what it recorded and frees its rows. */
    suspend fun undo(importId: String): Result<Unit>

    /** The latest live import of the account, or null when there is none. */
    suspend fun getLastImport(accountId: String): Result<BankImportSummary?>
}

/** The code the repository gives a statement row that was imported meanwhile (23505 on the reference). */
const val BANK_ROW_ALREADY_IMPORTED = "BANK_ROW_ALREADY_IMPORTED"
