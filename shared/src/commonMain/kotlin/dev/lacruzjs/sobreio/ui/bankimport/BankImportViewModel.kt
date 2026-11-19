package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.BankFormatStore
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.BankImportRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.bankimport.AmountMode
import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping
import dev.lacruzjs.sobreio.domain.bankimport.DateOrder
import dev.lacruzjs.sobreio.domain.bankimport.DecimalMark
import dev.lacruzjs.sobreio.domain.bankimport.StatementTable
import dev.lacruzjs.sobreio.domain.bankimport.decodeStatement
import dev.lacruzjs.sobreio.domain.bankimport.detectSeparator
import dev.lacruzjs.sobreio.domain.bankimport.guessDateOrder
import dev.lacruzjs.sobreio.domain.bankimport.guessDecimalMark
import dev.lacruzjs.sobreio.domain.bankimport.guessMapping
import dev.lacruzjs.sobreio.domain.bankimport.headerSignature
import dev.lacruzjs.sobreio.domain.bankimport.isComplete
import dev.lacruzjs.sobreio.domain.bankimport.parseCsv
import dev.lacruzjs.sobreio.domain.bankimport.readStatementRows
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_incomplete
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_no_rows
import dev.lacruzjs.sobreio.generated.resources.bank_import_file_too_large
import dev.lacruzjs.sobreio.generated.resources.bank_import_file_unreadable
import dev.lacruzjs.sobreio.generated.resources.bank_import_row_ignored
import dev.lacruzjs.sobreio.ui.bankimport.picker.PickResult
import dev.lacruzjs.sobreio.ui.theme.TagColor
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Imports a bank statement into one account (D-52), in steps on one route: Source (pick the file), Columns (only for a
 * format the app does not know), Review (what happens to each row) and Result. The app reads the CSV (D-37); the
 * database says what each row is, records the decisions in one call and undoes the import as a whole.
 */
class BankImportViewModel(
    private val groupId: String,
    private val accountId: String,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val detail: AccountDetailRepository,
    private val imports: BankImportRepository,
    private val formats: BankFormatStore,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BankImportUiState())
    val state: StateFlow<BankImportUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(importError = error) } },
    )

    init {
        load()
    }

    fun onEvent(event: BankImportEvent) {
        when (event) {
            BankImportEvent.Retry -> load()
            BankImportEvent.ChooseFileClicked -> _state.update { it.copy(sourceError = null) }
            is BankImportEvent.FilePicked -> onFilePicked(event.result)
            is BankImportEvent.ColumnTileToggled -> updateColumns {
                it.copy(openTile = if (it.openTile == event.tile) null else event.tile)
            }
            is BankImportEvent.ColumnChosen -> updateColumns { columns ->
                val m = columns.mapping
                val mapping = when (event.tile) {
                    ColumnTile.Date -> m.copy(dateColumn = event.column)
                    ColumnTile.Description -> m.copy(descriptionColumn = event.column)
                    ColumnTile.Amount -> m.copy(amountColumn = event.column)
                    ColumnTile.Charge -> m.copy(chargeColumn = event.column)
                    ColumnTile.Credit -> m.copy(creditColumn = event.column)
                    ColumnTile.Balance -> m.copy(balanceColumn = event.column)
                }
                columns.copy(mapping = mapping, openTile = null, error = null)
            }
            is BankImportEvent.AmountModeChosen -> updateColumns { it.copy(mapping = it.mapping.copy(amountMode = event.mode), error = null) }
            is BankImportEvent.DateOrderChosen -> updateColumns { it.copy(mapping = it.mapping.copy(dateOrder = event.order)) }
            is BankImportEvent.DecimalMarkChosen -> updateColumns { it.copy(mapping = it.mapping.copy(decimalMark = event.mark)) }
            BankImportEvent.ColumnsContinueClicked -> continueFromColumns()
            BankImportEvent.EditColumnsClicked -> editColumns()
            is BankImportEvent.RowClicked -> openSheet(event.index)
            is BankImportEvent.RowIgnoreSwiped -> ignoreRow(event.index)
            BankImportEvent.UndoClicked -> undo.undo()
            BankImportEvent.UndoExpired -> undo.commit()
            is BankImportEvent.ImportClicked -> import(event.tagName)
            BankImportEvent.SheetDismissed -> _state.update { it.copy(sheet = null) }
            is BankImportEvent.SheetActionChosen -> updateDraft { draft ->
                val pending = draft.pending.firstOrNull()
                val existing = draft.existing.firstOrNull()
                when (event.action) {
                    BankRowAction.Confirm -> draft.copy(
                        action = event.action,
                        commitmentId = draft.commitmentId ?: pending?.commitmentId,
                        dueDate = draft.dueDate ?: pending?.dueDate,
                    )
                    BankRowAction.Link -> draft.copy(
                        action = event.action,
                        linkId = draft.linkId ?: existing?.id,
                        linkSource = draft.linkSource ?: existing?.source,
                    )
                    else -> draft.copy(action = event.action)
                }
            }
            is BankImportEvent.SheetTransferChosen -> updateDraft { it.copy(isTransfer = event.isTransfer) }
            is BankImportEvent.SheetSelectToggled -> _state.update { s ->
                val sheet = s.sheet ?: return@update s
                s.copy(sheet = sheet.copy(openSelect = if (sheet.openSelect == event.select) null else event.select))
            }
            is BankImportEvent.SheetEnvelopeChosen -> updateDraft { it.copy(envelopeId = event.envelopeId) }
            is BankImportEvent.SheetOtherAccountChosen -> updateDraft { it.copy(otherAccountId = event.accountId) }
            is BankImportEvent.SheetPendingChosen -> updateDraft { draft ->
                val chosen = draft.pending.firstOrNull { pendingKey(it.commitmentId, it.dueDate.toString()) == event.key }
                draft.copy(commitmentId = chosen?.commitmentId, dueDate = chosen?.dueDate)
            }
            is BankImportEvent.SheetExistingChosen -> updateDraft { draft ->
                val chosen = draft.existing.firstOrNull { it.id == event.id }
                draft.copy(linkId = chosen?.id, linkSource = chosen?.source)
            }
            is BankImportEvent.SheetNotesChanged -> updateDraft { it.copy(notes = event.notes) }
            BankImportEvent.SheetDoneClicked -> applySheet()
            BankImportEvent.UndoImportClicked -> undoImport()
            BankImportEvent.SeeMovementsClicked -> _state.update { it.copy(openMovements = true) }
            BankImportEvent.DoneClicked -> _state.update { it.copy(close = true) }
            BankImportEvent.BackPressed -> back(closeAll = false)
            BankImportEvent.CloseClicked -> back(closeAll = true)
            BankImportEvent.DiscardConfirmed -> discard()
            BankImportEvent.DiscardCancelled -> _state.update { it.copy(showDiscardWarning = null) }
            BankImportEvent.NavigationHandled -> _state.update { it.copy(close = false, openMovements = false) }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val group = async { groups.getGroup(groupId) }
            val accountList = async { accounts.getAccounts(groupId) }
            val envelopeList = async { envelopes.getEnvelopes(groupId) }
            val today = async { calendar.getToday(groupId) }
            val last = async { imports.getLastImport(accountId) }
            val g = group.await().getOrElse { e -> return@launch fail(e) }
            val all = accountList.await().getOrElse { e -> return@launch fail(e) }
            val account = all.firstOrNull { it.id == accountId } ?: return@launch fail(dev.lacruzjs.sobreio.domain.error.AppError.NotFound)
            _state.update {
                it.copy(
                    isLoading = false,
                    groupName = g.name,
                    currencyCode = g.currencyCode,
                    account = account,
                    canImport = !g.isArchived && !g.isPendingDeletion && !account.isArchived,
                    today = today.await().getOrNull(),
                    lastImport = last.await().getOrNull(),
                    envelopes = envelopeList.await().getOrNull().orEmpty()
                        .filter { e -> !e.isArchived && e.kind != EnvelopeKind.System }
                        .sortedBy { e -> e.name.lowercase() },
                    otherAccounts = all.filter { a -> a.id != accountId && !a.isArchived }.sortedBy { a -> a.name.lowercase() },
                )
            }
        }
    }

    private fun fail(error: Throwable) {
        _state.update { it.copy(isLoading = false, error = error.asAppError()) }
    }

    // Source -----------------------------------------------------------------------------------------------------------

    private fun onFilePicked(result: PickResult) {
        when (result) {
            PickResult.Cancelled -> Unit
            PickResult.TooLarge -> _state.update { it.copy(sourceError = Res.string.bank_import_file_too_large) }
            PickResult.Unreadable -> _state.update { it.copy(sourceError = Res.string.bank_import_file_unreadable) }
            is PickResult.Picked -> {
                _state.update { it.copy(isWorking = true, sourceError = null) }
                viewModelScope.launch {
                    val text = decodeStatement(result.file.bytes)
                    val table = StatementTable(result.file.name, parseCsv(text, detectSeparator(text)))
                    if (table.rows.size < 2) {
                        _state.update { it.copy(isWorking = false, sourceError = Res.string.bank_import_file_unreadable) }
                        return@launch
                    }
                    val guessed = guessMapping(table)
                    val remembered = guessed?.let { formats.get(accountId, headerSignature(table.header(it.headerRow))) }
                    val mapping = remembered ?: guessed
                    _state.update { it.copy(fileName = result.file.name, table = table) }
                    if (remembered != null && remembered.isComplete(table.columnCount)) {
                        _state.update { it.copy(cameThroughColumns = false) }
                        preview(table, remembered)
                    } else {
                        showColumns(table, mapping ?: blankMapping(table))
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun blankMapping(table: StatementTable) = ColumnMapping(
        headerRow = 0, dateColumn = -1, descriptionColumn = -1, amountMode = AmountMode.OneColumn, amountColumn = -1,
        dateOrder = DateOrder.DayMonthYear, decimalMark = DecimalMark.Comma,
    )

    // Columns ----------------------------------------------------------------------------------------------------------

    private fun showColumns(table: StatementTable, mapping: ColumnMapping) {
        val body = table.body(mapping.headerRow).take(50)
        val dates = if (mapping.dateColumn >= 0) body.mapNotNull { it.getOrNull(mapping.dateColumn) } else emptyList()
        val amountColumns = listOf(mapping.amountColumn, mapping.chargeColumn, mapping.creditColumn).filter { it >= 0 }
        val amounts = body.flatMap { row -> amountColumns.mapNotNull { row.getOrNull(it) } }
        _state.update {
            it.copy(
                isWorking = false,
                step = ImportStep.Columns,
                cameThroughColumns = true,
                columns = ColumnsState(
                    mapping = mapping,
                    askDateOrder = dates.isEmpty() || guessDateOrder(dates).ambiguous,
                    askDecimalMark = amounts.isEmpty() || guessDecimalMark(amounts).ambiguous,
                ),
            )
        }
    }

    private fun updateColumns(transform: (ColumnsState) -> ColumnsState) {
        _state.update { s -> s.columns?.let { s.copy(columns = transform(it)) } ?: s }
    }

    private fun continueFromColumns() {
        val s = _state.value
        val table = s.table ?: return
        val columns = s.columns ?: return
        if (!columns.mapping.isComplete(table.columnCount)) {
            updateColumns { it.copy(error = Res.string.bank_import_columns_incomplete) }
            return
        }
        formats.set(accountId, headerSignature(table.header(columns.mapping.headerRow)), columns.mapping)
        _state.update { it.copy(isWorking = true) }
        viewModelScope.launch { preview(table, columns.mapping) }
    }

    private fun editColumns() {
        val s = _state.value
        val table = s.table ?: return
        val mapping = s.columns?.mapping ?: guessMapping(table) ?: blankMapping(table)
        // The decisions are kept until a new mapping reads the rows again
        showColumns(table, formats.get(accountId, headerSignature(table.header(mapping.headerRow))) ?: mapping)
    }

    // Preview ----------------------------------------------------------------------------------------------------------

    private suspend fun preview(table: StatementTable, mapping: ColumnMapping) {
        val read = readStatementRows(table, mapping)
        if (read.rows.isEmpty()) {
            _state.update {
                it.copy(
                    isWorking = false,
                    step = if (it.cameThroughColumns) ImportStep.Columns else ImportStep.Source,
                    sourceError = Res.string.bank_import_columns_no_rows,
                    columns = it.columns?.copy(error = Res.string.bank_import_columns_no_rows),
                )
            }
            return
        }
        imports.preview(accountId, read.rows)
            .onSuccess { previews ->
                val byIndex = previews.associateBy { it.index }
                val rows = read.rows.map { row -> importRow(row, byIndex[row.index]) }
                    .sortedByDescending { it.row.date }
                _state.update {
                    it.copy(
                        isWorking = false,
                        step = ImportStep.Review,
                        rows = rows,
                        skippedRows = read.skipped,
                        closingBalanceCents = read.closingBalanceCents,
                        importError = null,
                    )
                }
            }
            .onFailure { e -> _state.update { it.copy(isWorking = false, importError = e.asAppError(), step = ImportStep.Review, rows = emptyList()) } }
    }

    private fun importRow(row: dev.lacruzjs.sobreio.domain.bankimport.StatementRow, preview: BankRowPreview?): ImportRow {
        if (preview == null) return ImportRow(row = row, status = BankRowStatus.Imported, action = BankRowAction.Ignore)
        return ImportRow(
            row = row,
            status = preview.status,
            action = preview.suggestedAction,
            envelopeId = preview.envelopeId,
            commitmentId = preview.commitmentId,
            dueDate = preview.dueDate,
            linkSource = preview.matchSource,
            linkId = preview.matchId,
            pending = preview.pending,
            existing = preview.existing,
        )
    }

    // Review -----------------------------------------------------------------------------------------------------------

    private fun openSheet(index: Int) {
        val row = _state.value.rows.getOrNull(index) ?: return
        if (!row.isEditable) return
        _state.update { it.copy(sheet = RowSheetState(index, row, similarCount = similarRows(it.rows, index).size)) }
    }

    private fun updateDraft(transform: (ImportRow) -> ImportRow) {
        _state.update { s -> s.sheet?.let { s.copy(sheet = it.copy(draft = transform(it.draft))) } ?: s }
    }

    /** Other new rows with the same description and direction: choosing an envelope for one chooses it for them. */
    private fun similarRows(rows: List<ImportRow>, index: Int): List<Int> {
        val row = rows.getOrNull(index) ?: return emptyList()
        val key = row.row.description.lowercase()
        if (key.isBlank()) return emptyList()
        return rows.indices.filter { i ->
            val other = rows[i]
            i != index && other.isEditable && other.action == BankRowAction.Create && !other.isTransfer &&
                other.row.description.lowercase() == key && (other.row.amountCents < 0) == (row.row.amountCents < 0)
        }
    }

    private fun applySheet() {
        _state.update { s ->
            val sheet = s.sheet ?: return@update s
            var draft = sheet.draft
            // A choice without its target falls back to recording the row
            if (draft.action == BankRowAction.Confirm && draft.pendingChosen == null) draft = draft.copy(action = BankRowAction.Create)
            if (draft.action == BankRowAction.Link && draft.existingChosen == null) draft = draft.copy(action = BankRowAction.Create)
            if (draft.action == BankRowAction.Create && draft.isTransfer && draft.otherAccountId == null) draft = draft.copy(isTransfer = false)
            val previous = s.rows[sheet.index]
            val envelopeChanged = draft.action == BankRowAction.Create && !draft.isTransfer && draft.envelopeId != previous.envelopeId
            val similar = if (envelopeChanged) similarRows(s.rows, sheet.index).toSet() else emptySet()
            val rows = s.rows.mapIndexed { i, row ->
                when {
                    i == sheet.index -> draft
                    i in similar -> row.copy(envelopeId = draft.envelopeId)
                    else -> row
                }
            }
            s.copy(rows = rows, sheet = null)
        }
    }

    private fun ignoreRow(index: Int) {
        val row = _state.value.rows.getOrNull(index) ?: return
        if (!row.isEditable || row.action == BankRowAction.Ignore) return
        val previousAction = row.action
        setAction(index, BankRowAction.Ignore)
        // Nothing is written until the import: the snackbar only offers to take the row back
        undo.show(
            UndoableAction(
                message = Res.string.bank_import_row_ignored,
                argument = row.row.description,
                commit = { Result.success(Unit) },
                undo = { Result.success(Unit) },
                restore = { setAction(index, previousAction) },
            ),
        )
    }

    private fun setAction(index: Int, action: BankRowAction) {
        _state.update { s -> s.copy(rows = s.rows.mapIndexed { i, r -> if (i == index) r.copy(action = action) else r }) }
    }

    private fun import(tagName: String) {
        val s = _state.value
        if (s.isImporting || s.sentCount == 0) return
        undo.commit()
        _state.update { it.copy(isImporting = true, importError = null) }
        val choices = s.rows.filter { it.isSent }.map { it.toChoice() }
        viewModelScope.launch {
            imports.apply(accountId, s.fileName, tagName, TagColor.Default.hex, choices)
                .onSuccess { summary ->
                    val closing = s.closingBalanceCents
                    val comparison = closing?.let { detail.compareWithBank(accountId, it).getOrNull() }
                    _state.update {
                        it.copy(isImporting = false, step = ImportStep.Result, result = summary, comparison = comparison, lastImport = summary)
                    }
                }
                .onFailure { e -> _state.update { it.copy(isImporting = false, importError = e.asAppError()) } }
        }
    }

    private fun ImportRow.toChoice() = BankRowChoice(
        ref = row.ref,
        date = row.date,
        amountCents = row.amountCents,
        description = row.description,
        action = action,
        kind = if (isTransfer) EntryKind.Transfer else if (row.amountCents < 0) EntryKind.Expense else EntryKind.Income,
        envelopeId = envelopeId.takeIf { !isTransfer },
        otherAccountId = otherAccountId.takeIf { isTransfer },
        notes = notes.trim().takeIf { it.isNotEmpty() },
        commitmentId = commitmentId,
        dueDate = dueDate,
        linkSource = linkSource,
        linkId = linkId,
    )

    // Result -----------------------------------------------------------------------------------------------------------

    private fun undoImport() {
        val result = _state.value.result ?: return
        if (_state.value.isUndoing || _state.value.undone) return
        _state.update { it.copy(isUndoing = true, undoError = null) }
        viewModelScope.launch {
            imports.undo(result.id)
                .onSuccess {
                    val last = imports.getLastImport(accountId).getOrNull()
                    _state.update { it.copy(isUndoing = false, undone = true, lastImport = last) }
                }
                .onFailure { e -> _state.update { it.copy(isUndoing = false, undoError = e.asAppError()) } }
        }
    }

    // Leaving ----------------------------------------------------------------------------------------------------------

    private fun back(closeAll: Boolean) {
        val s = _state.value
        if (s.sheet != null && !closeAll) {
            _state.update { it.copy(sheet = null) }
            return
        }
        if (s.isImporting) return
        when {
            s.hasReviewWork -> _state.update {
                it.copy(showDiscardWarning = if (closeAll) DiscardTarget.Close else DiscardTarget.PreviousStep)
            }
            closeAll || s.step == ImportStep.Source || s.step == ImportStep.Result -> _state.update { it.copy(close = true) }
            s.step == ImportStep.Columns -> toSource()
            else -> previousStepFromReview()
        }
    }

    private fun discard() {
        val target = _state.value.showDiscardWarning ?: return
        undo.commit()
        _state.update { it.copy(showDiscardWarning = null) }
        if (target == DiscardTarget.Close) _state.update { it.copy(close = true) } else previousStepFromReview()
    }

    private fun previousStepFromReview() {
        val s = _state.value
        val table = s.table
        if (s.cameThroughColumns && table != null && s.columns != null) {
            _state.update { it.copy(step = ImportStep.Columns, rows = emptyList(), sheet = null, importError = null) }
        } else {
            toSource()
        }
    }

    private fun toSource() {
        _state.update {
            it.copy(
                step = ImportStep.Source, table = null, columns = null, rows = emptyList(), fileName = "",
                cameThroughColumns = false, sheet = null, importError = null, closingBalanceCents = null,
            )
        }
    }

    override fun onCleared() {
        undo.commitDetached()
    }
}

/** How a pending candidate is told apart in the sheet's choice: one commitment can be due twice in the window. */
fun pendingKey(commitmentId: String, dueDate: String) = "$commitmentId|$dueDate"
