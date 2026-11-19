// Bank import, Review step: the statement rows in sections by what will happen to them.

package dev.lacruzjs.sobreio.ui.bankimport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.bank_import_import_rows
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_before_opening
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_confirms
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_future
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_ignored
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_imported
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_links
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_outside
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_to_envelope
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_transfer_from
import dev.lacruzjs.sobreio.generated.resources.bank_import_line_transfer_to
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_range
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_skipped
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_due
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_ignored
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_imported
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_new
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_recorded
import dev.lacruzjs.sobreio.generated.resources.bank_import_tag_name
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.ignoreAction
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Review ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun ReviewStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val year = state.today?.year ?: 0
    var openRow by remember { mutableStateOf<Int?>(null) }
    var showImported by rememberSaveable { mutableStateOf(false) }
    var showIgnored by rememberSaveable { mutableStateOf(false) }
    val indexed = state.rows.withIndex().toList()
    fun rowsOf(section: ImportSection) = indexed.filter { it.value.section == section }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.isWorking) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        val first = state.rows.minOfOrNull { it.row.date }
                        val last = state.rows.maxOfOrNull { it.row.date }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (first != null && last != null) {
                                Text(
                                    pluralStringResource(
                                        Res.plurals.bank_import_review_range,
                                        state.rows.size,
                                        formatShortDate(first, languageTag, year),
                                        formatShortDate(last, languageTag, year),
                                        state.rows.size,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.skippedRows > 0) {
                                Text(
                                    pluralStringResource(Res.plurals.bank_import_review_skipped, state.skippedRows, state.skippedRows),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            ErrorText(state.importError)
                        }
                    }
                    rowSection(Res.string.bank_import_section_due, rowsOf(ImportSection.Due), state, openRow, { openRow = it }, onEvent)
                    rowSection(Res.string.bank_import_section_recorded, rowsOf(ImportSection.Recorded), state, openRow, { openRow = it }, onEvent)
                    rowSection(Res.string.bank_import_section_new, rowsOf(ImportSection.New), state, openRow, { openRow = it }, onEvent)
                    foldedSection(Res.string.bank_import_section_ignored, rowsOf(ImportSection.Ignored), showIgnored, { showIgnored = !showIgnored }, state, onEvent)
                    foldedSection(Res.string.bank_import_section_imported, rowsOf(ImportSection.AlreadyImported), showImported, { showImported = !showImported }, state, onEvent)
                }
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage,
            onUndo = { onEvent(BankImportEvent.UndoClicked) },
            onDismiss = { onEvent(BankImportEvent.UndoExpired) },
        )
        if (state.rows.isNotEmpty()) {
            val tagName = stringResource(Res.string.bank_import_tag_name)
            PrimaryButton(
                text = pluralStringResource(Res.plurals.bank_import_import_rows, state.recordCount, state.recordCount),
                onClick = { onEvent(BankImportEvent.ImportClicked(tagName)) },
                isLoading = state.isImporting,
                enabled = state.sentCount > 0 && !state.isWorking,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

private fun LazyListScope.rowSection(
    title: org.jetbrains.compose.resources.StringResource,
    rows: List<IndexedValue<ImportRow>>,
    state: BankImportUiState,
    openRow: Int?,
    onOpenRow: (Int?) -> Unit,
    onEvent: (BankImportEvent) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "title-$title") { SectionTitle(stringResource(title, rows.size)) }
    items(rows, key = { it.value.row.ref }) { (index, row) ->
        SwipeActionsRow(
            actions = listOf(ignoreAction { onEvent(BankImportEvent.RowIgnoreSwiped(index)); onOpenRow(null) }),
            isOpen = openRow == index,
            onOpenChange = { open -> onOpenRow(if (open) index else null) },
            onClick = { onEvent(BankImportEvent.RowClicked(index)) },
        ) {
            ImportRowTile(row, state)
        }
    }
}

private fun LazyListScope.foldedSection(
    title: org.jetbrains.compose.resources.StringResource,
    rows: List<IndexedValue<ImportRow>>,
    open: Boolean,
    onToggle: () -> Unit,
    state: BankImportUiState,
    onEvent: (BankImportEvent) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "folded-$title") {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(title, rows.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
    }
    if (open) {
        items(rows, key = { it.value.row.ref }) { (index, row) ->
            Box(modifier = Modifier.fillMaxWidth().clickable(enabled = row.isEditable) { onEvent(BankImportEvent.RowClicked(index)) }) {
                ImportRowTile(row, state)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Concept from the bank, what will happen with it, and the amount with its day. */
@Composable
private fun ImportRowTile(row: ImportRow, state: BankImportUiState) {
    val languageTag = Locale.current.toLanguageTag()
    val year = state.today?.year ?: 0
    SoftTile {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(row.row.description, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    decisionLine(row, state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    amountCents = row.row.amountCents,
                    currencyCode = state.currencyCode,
                    role = if (row.isTransfer) MoneyRole.Transfer else MoneyRole.Flow,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
                Text(
                    formatShortDate(row.row.date, languageTag, year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The one muted line that says what will be done with the row. */
@Composable
internal fun decisionLine(row: ImportRow, state: BankImportUiState): String {
    val languageTag = Locale.current.toLanguageTag()
    val year = state.today?.year ?: 0
    return when {
        row.status == BankRowStatus.Imported -> stringResource(Res.string.bank_import_line_imported)
        row.status == BankRowStatus.Future -> stringResource(Res.string.bank_import_line_future)
        row.action == BankRowAction.Ignore && row.status == BankRowStatus.BeforeOpening ->
            stringResource(Res.string.bank_import_line_before_opening)
        row.action == BankRowAction.Ignore -> stringResource(Res.string.bank_import_line_ignored)
        row.action == BankRowAction.Confirm -> {
            val pending = row.pendingChosen
            stringResource(Res.string.bank_import_line_confirms, pending?.name.orEmpty(), pending?.let { formatShortDate(it.dueDate, languageTag, year) }.orEmpty())
        }
        row.action == BankRowAction.Link -> {
            val existing = row.existingChosen
            val name = existing?.notes?.takeIf { it.isNotBlank() } ?: row.row.description
            stringResource(Res.string.bank_import_line_links, name, existing?.let { formatShortDate(it.date, languageTag, year) }.orEmpty())
        }
        row.isTransfer -> {
            val other = state.otherAccounts.firstOrNull { it.id == row.otherAccountId }?.name.orEmpty()
            stringResource(if (row.row.amountCents < 0) Res.string.bank_import_line_transfer_to else Res.string.bank_import_line_transfer_from, other)
        }
        state.account?.inEnvelopes == false -> stringResource(Res.string.bank_import_line_outside)
        else -> stringResource(
            Res.string.bank_import_line_to_envelope,
            state.envelopes.firstOrNull { it.id == row.envelopeId }?.name ?: stringResource(Res.string.envelopes_unassigned),
        )
    }
}
