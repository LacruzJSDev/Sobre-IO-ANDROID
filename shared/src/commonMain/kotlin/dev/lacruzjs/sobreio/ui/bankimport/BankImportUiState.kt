package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping
import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.bankimport.StatementTable
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.BankImportSummary
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.ExistingCandidate
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.PendingCandidate
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/** The steps of the import, one route (D-52). Columns is shown only for a format the app does not know. */
enum class ImportStep { Source, Columns, Review, Result }

/** Where a row sits in the review: by what will happen to it. */
enum class ImportSection { Due, Recorded, New, Ignored, AlreadyImported }

/**
 * One statement row and what will be done with it: the database's suggestion first, then the user's choice. Only
 * [status] [BankRowStatus.New] and [BankRowStatus.BeforeOpening] rows can change; an expense or an income follows the
 * sign of the amount, [isTransfer] makes it a transfer with [otherAccountId]. [envelopeId] null is Unassigned.
 */
data class ImportRow(
    val row: StatementRow,
    val status: BankRowStatus,
    val action: BankRowAction,
    val isTransfer: Boolean = false,
    val envelopeId: String? = null,
    val otherAccountId: String? = null,
    val notes: String = row.description,
    val commitmentId: String? = null,
    val dueDate: LocalDate? = null,
    val linkSource: MovementSource? = null,
    val linkId: String? = null,
    val pending: List<PendingCandidate> = emptyList(),
    val existing: List<ExistingCandidate> = emptyList(),
) {
    val section: ImportSection get() = when {
        status == BankRowStatus.Imported -> ImportSection.AlreadyImported
        status == BankRowStatus.Future -> ImportSection.Ignored
        action == BankRowAction.Confirm -> ImportSection.Due
        action == BankRowAction.Link -> ImportSection.Recorded
        action == BankRowAction.Create -> ImportSection.New
        else -> ImportSection.Ignored
    }

    /** Rows already imported or in the future cannot be changed: the database would reject them. */
    val isEditable: Boolean get() = status == BankRowStatus.New || status == BankRowStatus.BeforeOpening

    /** Sent to the database: everything except rows already imported and future ones (kept for a later import). */
    val isSent: Boolean get() = isEditable

    /** Records something: created, confirmed or linked. */
    val records: Boolean get() = isSent && action != BankRowAction.Ignore

    val pendingChosen: PendingCandidate? get() = pending.firstOrNull { it.commitmentId == commitmentId && it.dueDate == dueDate }

    val existingChosen: ExistingCandidate? get() = existing.firstOrNull { it.id == linkId }
}

/** The Columns step: the table, the mapping being chosen and which choices the user has to see. */
data class ColumnsState(
    val mapping: ColumnMapping,
    /** Shown only when the dates or amounts fit two readings. */
    val askDateOrder: Boolean,
    val askDecimalMark: Boolean,
    val error: StringResource? = null,
    /** One expandable tile open at a time. */
    val openTile: ColumnTile? = null,
)

enum class ColumnTile { Date, Description, Amount, Charge, Credit, Balance }

/** The one-row sheet while it is open: a copy of the row being changed. */
data class RowSheetState(
    val index: Int,
    val draft: ImportRow,
    /** Other new rows with the same description that take the envelope too. */
    val similarCount: Int,
    val openSelect: RowSelect? = null,
)

enum class RowSelect { Envelope, OtherAccount, Pending, Existing }

/** Where a confirmed discard leads. */
enum class DiscardTarget { PreviousStep, Close }

data class BankImportUiState(
    val step: ImportStep = ImportStep.Source,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val groupName: String = "",
    val currencyCode: String = "EUR",
    val account: Account? = null,
    /** A live account of a live group: anyone in the group may import. */
    val canImport: Boolean = false,
    val today: LocalDate? = null,
    val lastImport: BankImportSummary? = null,
    /** Live envelopes other than the system ones, for the envelope choice. */
    val envelopes: List<Envelope> = emptyList(),
    /** The group's other live accounts, for a transfer. */
    val otherAccounts: List<Account> = emptyList(),
    /** Reading the file or asking the database for the preview. */
    val isWorking: Boolean = false,
    /** A file that could not be used (too large, unreadable, no rows). */
    val sourceError: StringResource? = null,
    val fileName: String = "",
    val table: StatementTable? = null,
    val columns: ColumnsState? = null,
    /** The Columns step was shown for this file, so back from the review returns to it. */
    val cameThroughColumns: Boolean = false,
    val rows: List<ImportRow> = emptyList(),
    /** Statement rows with no date or amount, left out. */
    val skippedRows: Int = 0,
    /** The bank's balance at the end of the statement, when the file has a balance column. */
    val closingBalanceCents: Long? = null,
    val sheet: RowSheetState? = null,
    val showDiscardWarning: DiscardTarget? = null,
    val isImporting: Boolean = false,
    val importError: AppError? = null,
    val result: BankImportSummary? = null,
    val comparison: BankComparison? = null,
    val isUndoing: Boolean = false,
    val undone: Boolean = false,
    val undoError: AppError? = null,
    /** Snackbar of an ignored row. */
    val undoMessage: UndoMessage? = null,
    /** Navigation results the Route turns into back stack changes. */
    val close: Boolean = false,
    /** Close and open the account's movements filtered by the import's tag. */
    val openMovements: Boolean = false,
) {
    fun section(section: ImportSection): List<ImportRow> = rows.filter { it.section == section }

    /** Rows that will record something: the count on the import button. */
    val recordCount: Int get() = rows.count { it.records }

    val sentCount: Int get() = rows.count { it.isSent }

    /** The review holds the user's work: leaving it warns first. */
    val hasReviewWork: Boolean get() = step == ImportStep.Review && rows.any { it.isSent }
}
