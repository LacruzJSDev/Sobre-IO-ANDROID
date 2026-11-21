package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.LabelTooltip
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_close
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.bank_import_choose_file
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard_text
import dev.lacruzjs.sobreio.generated.resources.bank_import_discard_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_edit_columns
import dev.lacruzjs.sobreio.generated.resources.bank_import_keep_reviewing
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import_value
import dev.lacruzjs.sobreio.generated.resources.bank_import_no_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_read_only
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_undone_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_review_title
import dev.lacruzjs.sobreio.generated.resources.bank_import_since
import dev.lacruzjs.sobreio.generated.resources.bank_import_source_help
import dev.lacruzjs.sobreio.generated.resources.bank_import_source_help_excel
import dev.lacruzjs.sobreio.generated.resources.bank_import_title
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.bankimport.picker.rememberStatementFilePicker
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatShortDate
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
        ImportStep.Result -> if (state.undone) Res.string.bank_import_result_undone_title else Res.string.bank_import_result_title
    }
    // The first and last steps close the import (X); the middle ones go back a step (arrow)
    val closes = state.step == ImportStep.Source || state.step == ImportStep.Result
    ScreenHeader(
        title = stringResource(title),
        subtitle = state.account?.name,
        onBack = if (closes) null else ({ onEvent(BankImportEvent.BackPressed) }),
        navigation = if (closes) {
            {
                LabelTooltip(stringResource(Res.string.action_close)) {
                    IconButton(onClick = { onEvent(BankImportEvent.CloseClicked) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                    }
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
                    pluralStringResource(
                        Res.plurals.bank_import_last_import_value,
                        last.rowsTotal,
                        formatShortDate(last.createdDay, languageTag, year),
                        last.rowsTotal,
                    )
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
        if (!state.canImport) InlineNotice(stringResource(Res.string.bank_import_read_only))
        state.sourceError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
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
