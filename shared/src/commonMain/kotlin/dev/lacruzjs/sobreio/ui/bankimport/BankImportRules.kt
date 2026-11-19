package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.domain.bankimport.AmountMode
import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping
import dev.lacruzjs.sobreio.domain.bankimport.DateOrder
import dev.lacruzjs.sobreio.domain.bankimport.DecimalMark
import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.bankimport.StatementTable
import dev.lacruzjs.sobreio.domain.bankimport.guessDateOrder
import dev.lacruzjs.sobreio.domain.bankimport.guessDecimalMark
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.EntryKind

/*
 * The bank import's rules on the user's choices, as pure functions: what the Columns step asks, how the database's
 * preview becomes a row, how the row sheet's answer is applied and what is sent. Reading the file itself lives in
 * `domain/bankimport`; the ViewModel only calls these and talks to the repositories.
 */

/** Nothing chosen yet: every column is to be picked in the Columns step. */
internal val BlankMapping = ColumnMapping(
    headerRow = 0, dateColumn = -1, descriptionColumn = -1, amountMode = AmountMode.OneColumn, amountColumn = -1,
    dateOrder = DateOrder.DayMonthYear, decimalMark = DecimalMark.Comma,
)

/** How many body rows the Columns step reads to decide whether the day order and decimal mark are ambiguous. */
private const val SAMPLE_ROWS = 50

/** The Columns step for [mapping]: the day order and the decimal mark are asked only when the file fits two readings. */
internal fun columnsStateFor(table: StatementTable, mapping: ColumnMapping): ColumnsState {
    val body = table.body(mapping.headerRow).take(SAMPLE_ROWS)
    val dates = if (mapping.dateColumn >= 0) body.mapNotNull { it.getOrNull(mapping.dateColumn) } else emptyList()
    val amountColumns = listOf(mapping.amountColumn, mapping.chargeColumn, mapping.creditColumn).filter { it >= 0 }
    val amounts = body.flatMap { row -> amountColumns.mapNotNull { row.getOrNull(it) } }
    return ColumnsState(
        mapping = mapping,
        askDateOrder = dates.isEmpty() || guessDateOrder(dates).ambiguous,
        askDecimalMark = amounts.isEmpty() || guessDecimalMark(amounts).ambiguous,
    )
}

/** A statement row with what the database suggests; a row it does not answer for was imported before and is ignored. */
internal fun importRowOf(row: StatementRow, preview: BankRowPreview?): ImportRow {
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

/**
 * Other new rows with the same description (ignoring case) and the same direction as the row at [index]: choosing an
 * envelope for one chooses it for them.
 */
internal fun similarRows(rows: List<ImportRow>, index: Int): List<Int> {
    val row = rows.getOrNull(index) ?: return emptyList()
    val key = row.row.description.lowercase()
    if (key.isBlank()) return emptyList()
    return rows.indices.filter { i ->
        val other = rows[i]
        i != index && other.isEditable && other.action == BankRowAction.Create && !other.isTransfer &&
            other.row.description.lowercase() == key && (other.row.amountCents < 0) == (row.row.amountCents < 0)
    }
}

/**
 * The rows after the sheet of one row is applied. A choice without its target falls back to recording the row (a
 * confirm with no due date, a link with no movement, a transfer with no other account), and a new envelope goes to the
 * similar rows too.
 */
internal fun applyRowSheet(rows: List<ImportRow>, sheet: RowSheetState): List<ImportRow> {
    var draft = sheet.draft
    if (draft.action == BankRowAction.Confirm && draft.pendingChosen == null) draft = draft.copy(action = BankRowAction.Create)
    if (draft.action == BankRowAction.Link && draft.existingChosen == null) draft = draft.copy(action = BankRowAction.Create)
    if (draft.action == BankRowAction.Create && draft.isTransfer && draft.otherAccountId == null) draft = draft.copy(isTransfer = false)
    val previous = rows[sheet.index]
    val envelopeChanged = draft.action == BankRowAction.Create && !draft.isTransfer && draft.envelopeId != previous.envelopeId
    val similar = if (envelopeChanged) similarRows(rows, sheet.index).toSet() else emptySet()
    return rows.mapIndexed { i, row ->
        when {
            i == sheet.index -> draft
            i in similar -> row.copy(envelopeId = draft.envelopeId)
            else -> row
        }
    }
}

/** What `apply_bank_import` receives for a row: the sign of the amount gives the kind unless it is a transfer. */
internal fun ImportRow.toChoice() = BankRowChoice(
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
