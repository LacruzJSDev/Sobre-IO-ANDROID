package dev.lacruzjs.sobreio.ui.bankimport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_close
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.bank_import_choose_file
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_continue
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard_text
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_done
import dev.lacruzjs.sobreio.generated.resources.bank_import_edit_columns
import dev.lacruzjs.sobreio.generated.resources.bank_import_import_rows
import dev.lacruzjs.sobreio.generated.resources.bank_import_keep_reviewing
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import_value
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
import dev.lacruzjs.sobreio.generated.resources.bank_import_no_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_read_only
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_bank_balance
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_app_balance
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_confirmed
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_created
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_difference
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_ignored
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_linked
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_matches
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_mismatch_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_undone
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_range
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_skipped
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_due
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_ignored
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_imported
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_new
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_recorded
import dev.lacruzjs.sobreio.generated.resources.bank_import_see_movements
import dev.lacruzjs.sobreio.generated.resources.bank_import_since
import dev.lacruzjs.sobreio.generated.resources.bank_import_source_help
import dev.lacruzjs.sobreio.generated.resources.bank_import_source_help_excel
import dev.lacruzjs.sobreio.generated.resources.bank_import_tag_name
import dev.lacruzjs.sobreio.generated.resources.bank_import_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_undo_import
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.bankimport.picker.rememberStatementFilePicker
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth
import dev.lacruzjs.sobreio.ui.components.ignoreAction
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 3 of the Accounts tab, full screen: imports a bank statement into the account (D-52). Holds the back stack
 * lambdas and the system file picker. Back goes one step back; leaving a review with rows warns first.
 */
@Composable
fun BankImportRoute(
    groupId: String,
    accountId: String,
    onClose: () -> Unit,
    onOpenMovements: (tagId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BankImportViewModel = koinViewModel { parametersOf(groupId, accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickFile = rememberStatementFilePicker { viewModel.onEvent(BankImportEvent.FilePicked(it)) }
    LaunchedEffect(state.close, state.openMovements) {
        when {
            state.openMovements -> {
                viewModel.onEvent(BankImportEvent.NavigationHandled)
                onOpenMovements(state.result?.tagId)
            }
            state.close -> {
                viewModel.onEvent(BankImportEvent.NavigationHandled)
                onClose()
            }
        }
    }
    // System back and the back gesture go one step back (or warn) instead of leaving the import at once
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { viewModel.onEvent(BankImportEvent.BackPressed) },
    )
    BankImportScreen(
        state = state,
        onEvent = { event ->
            if (event == BankImportEvent.ChooseFileClicked) pickFile()
            viewModel.onEvent(event)
        },
        modifier = modifier,
    )
}

@Composable
fun BankImportScreen(
    state: BankImportUiState,
    onEvent: (BankImportEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = if (state.step == ImportStep.Review) ContentWidth.List else ContentWidth.Form
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().contentMaxWidth(width)) {
            Header(state, onEvent)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(BankImportEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
                    }
                    else -> when (state.step) {
                        ImportStep.Source -> SourceStep(state, onEvent)
                        ImportStep.Columns -> ColumnsStep(state, onEvent)
                        ImportStep.Review -> ReviewStep(state, onEvent)
                        ImportStep.Result -> ResultStep(state, onEvent)
                    }
                }
            }
        }
    }
    RowSheet(state, onEvent)
    DiscardWarning(state, onEvent)
}

@Composable
private fun Header(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val title = when (state.step) {
        ImportStep.Source -> Res.string.bank_import_title
        ImportStep.Columns -> Res.string.bank_import_columns_title
        ImportStep.Review -> Res.string.bank_import_review_title
        ImportStep.Result -> Res.string.bank_import_result_title
    }
    // The first and last steps close the import (X); the middle ones go back a step (arrow)
    val closes = state.step == ImportStep.Source || state.step == ImportStep.Result
    ScreenHeader(
        title = stringResource(title),
        subtitle = state.account?.name,
        onBack = if (closes) null else ({ onEvent(BankImportEvent.BackPressed) }),
        navigation = if (closes) {
            {
                IconButton(onClick = { onEvent(BankImportEvent.CloseClicked) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                }
            }
        } else {
            null
        },
        actions = {
            if (state.step == ImportStep.Review && !state.isImporting) {
                IconActionButton(
                    icon = Icons.Filled.ViewColumn,
                    label = stringResource(Res.string.bank_import_edit_columns),
                    onClick = { onEvent(BankImportEvent.EditColumnsClicked) },
                )
            }
        },
    )
}

// Source ---------------------------------------------------------------------------------------------------------------

@Composable
private fun SourceStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val year = state.today?.year ?: 0
    StepColumn(
        bottom = {
            if (state.canImport) {
                PrimaryButton(
                    text = stringResource(Res.string.bank_import_choose_file),
                    onClick = { onEvent(BankImportEvent.ChooseFileClicked) },
                    isLoading = state.isWorking,
                )
            }
        },
    ) {
        SoftTile {
            Text(stringResource(Res.string.bank_import_source_help), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.bank_import_source_help_excel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SoftTile {
            val last = state.lastImport
            LabelValue(
                label = stringResource(Res.string.bank_import_last_import),
                value = if (last?.createdDay != null) {
                    stringResource(Res.string.bank_import_last_import_value, formatShortDate(last.createdDay, languageTag, year), last.rowsTotal)
                } else {
                    stringResource(Res.string.bank_import_no_last_import)
                },
            )
            last?.lastDate?.let {
                Text(
                    stringResource(Res.string.bank_import_since, formatShortDate(it, languageTag, year)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!state.canImport) FormNote(stringResource(Res.string.bank_import_read_only))
        state.sourceError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

// Review ---------------------------------------------------------------------------------------------------------------

@Composable
private fun ReviewStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
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
                                    stringResource(
                                        Res.string.bank_import_review_range,
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

// Result ---------------------------------------------------------------------------------------------------------------

@Composable
private fun ResultStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val result = state.result ?: return
    StepColumn(
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ErrorText(state.undoError)
                if (!state.undone) {
                    SecondaryButton(
                        text = stringResource(Res.string.bank_import_undo_import),
                        onClick = { onEvent(BankImportEvent.UndoImportClicked) },
                        isLoading = state.isUndoing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (result.tagId != null) {
                        PrimaryButton(
                            text = stringResource(Res.string.bank_import_see_movements),
                            onClick = { onEvent(BankImportEvent.SeeMovementsClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (state.undone || result.tagId == null) {
                    PrimaryButton(
                        text = stringResource(Res.string.bank_import_done),
                        onClick = { onEvent(BankImportEvent.DoneClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        if (state.undone) {
            FormNote(stringResource(Res.string.bank_import_result_undone))
        }
        SoftTile {
            LabelValue(stringResource(Res.string.bank_import_result_created), result.created.toString())
            LabelValue(stringResource(Res.string.bank_import_result_confirmed), result.confirmed.toString())
            LabelValue(stringResource(Res.string.bank_import_result_linked), result.linked.toString())
            LabelValue(stringResource(Res.string.bank_import_result_ignored), result.ignored.toString(), muted = true)
        }
        val comparison = state.comparison
        if (comparison != null && !state.undone) {
            val matches = comparison.differenceCents == 0L
            StatCard(
                rows = buildList {
                    add(StatRow(stringResource(Res.string.bank_import_result_bank_balance), comparison.bankBalanceCents, MoneyRole.Balance))
                    add(StatRow(stringResource(Res.string.bank_import_result_app_balance), comparison.appBalanceCents, MoneyRole.Balance))
                    if (!matches) add(StatRow(stringResource(Res.string.bank_import_result_difference), comparison.differenceCents, MoneyRole.Balance, emphasised = true))
                },
                currencyCode = state.currencyCode,
            )
            Text(
                stringResource(if (matches) Res.string.bank_import_result_matches else Res.string.bank_import_result_mismatch_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Shared pieces --------------------------------------------------------------------------------------------------------

/** A scrolling step with a fixed area at the bottom for its buttons. */
@Composable
internal fun StepColumn(bottom: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) { bottom() }
    }
}

@Composable
internal fun LabelValue(label: String, value: String, muted: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DiscardWarning(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    if (state.showDiscardWarning == null) return
    AlertDialog(
        onDismissRequest = { onEvent(BankImportEvent.DiscardCancelled) },
        title = { Text(stringResource(Res.string.bank_import_discard_title)) },
        text = { Text(stringResource(Res.string.bank_import_discard_text)) },
        confirmButton = {
            TextButton(onClick = { onEvent(BankImportEvent.DiscardConfirmed) }) {
                Text(stringResource(Res.string.bank_import_discard), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(BankImportEvent.DiscardCancelled) }) { Text(stringResource(Res.string.bank_import_keep_reviewing)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    )
}

