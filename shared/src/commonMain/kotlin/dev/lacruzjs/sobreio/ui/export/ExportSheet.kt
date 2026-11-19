package dev.lacruzjs.sobreio.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.export.CsvDialect
import dev.lacruzjs.sobreio.domain.export.ExportRecordType
import dev.lacruzjs.sobreio.domain.export.ExportRow
import dev.lacruzjs.sobreio.domain.export.buildExportCsv
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.export_action
import dev.lacruzjs.sobreio.generated.resources.export_col_account
import dev.lacruzjs.sobreio.generated.resources.export_col_amount
import dev.lacruzjs.sobreio.generated.resources.export_col_author
import dev.lacruzjs.sobreio.generated.resources.export_col_date
import dev.lacruzjs.sobreio.generated.resources.export_col_envelope
import dev.lacruzjs.sobreio.generated.resources.export_col_notes
import dev.lacruzjs.sobreio.generated.resources.export_col_tags
import dev.lacruzjs.sobreio.generated.resources.export_col_to_account
import dev.lacruzjs.sobreio.generated.resources.export_col_to_envelope
import dev.lacruzjs.sobreio.generated.resources.export_col_type
import dev.lacruzjs.sobreio.generated.resources.export_note
import dev.lacruzjs.sobreio.generated.resources.export_period_label
import dev.lacruzjs.sobreio.generated.resources.export_period_range
import dev.lacruzjs.sobreio.generated.resources.export_save_failed
import dev.lacruzjs.sobreio.generated.resources.export_saved
import dev.lacruzjs.sobreio.generated.resources.export_title
import dev.lacruzjs.sobreio.generated.resources.movement_kind_adjustment
import dev.lacruzjs.sobreio.generated.resources.movement_kind_expense
import dev.lacruzjs.sobreio.generated.resources.movement_kind_group_transfer_in
import dev.lacruzjs.sobreio.generated.resources.movement_kind_group_transfer_out
import dev.lacruzjs.sobreio.generated.resources.movement_kind_income
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_commitment_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_cover
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_manual
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_release_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_target
import dev.lacruzjs.sobreio.ui.components.ChipFormTile
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import org.jetbrains.compose.resources.stringResource

/**
 * Everything the export needs from the screen: the sheet when it is open, and the step that turns the database's rows
 * into the CSV (translated header and types, the user's number format) and hands it to the platform's save dialog.
 * Placed once by the Route that owns [viewModel].
 */
@Composable
fun ExportHost(viewModel: ExportViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saver = rememberCsvSaver { result -> viewModel.onEvent(ExportEvent.FileHandled(result)) }
    val languageTag = Locale.current.toLanguageTag()
    val header = exportHeader()
    val kindLabels = exportKindLabels()
    val latestSaver by rememberUpdatedState(saver)
    LaunchedEffect(state.pendingRows) {
        val rows = state.pendingRows ?: return@LaunchedEffect
        val fileName = state.pendingFileName ?: return@LaunchedEffect
        val csv = buildExportCsv(rows, header, { kindLabels.labelFor(it) }, CsvDialect.forLanguage(languageTag))
        latestSaver(fileName, csv)
    }
    if (state.open) ExportSheet(state, viewModel::onEvent)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExportSheet(state: ExportUiState, onEvent: (ExportEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    FlatBottomSheet(title = stringResource(Res.string.export_title), onDismiss = { onEvent(ExportEvent.Dismissed) }) {
        Text(
            text = stringResource(Res.string.export_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        ChipFormTile(label = stringResource(Res.string.export_period_label)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.periods.forEach { period ->
                    FormChip(
                        label = stringResource(
                            Res.string.export_period_range,
                            formatDayMonth(period.start, languageTag),
                            formatDayMonth(period.end, languageTag),
                        ),
                        selected = period.id == state.selectedPeriodId,
                        enabled = !state.isExporting,
                        onClick = { onEvent(ExportEvent.PeriodSelected(period.id)) },
                    )
                }
            }
        }
        ErrorText(state.error, modifier = Modifier.fillMaxWidth())
        when {
            state.saved -> Text(
                text = stringResource(Res.string.export_saved),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            state.saveFailed -> Text(
                text = stringResource(Res.string.export_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        PrimaryButton(
            text = stringResource(Res.string.export_action),
            onClick = { onEvent(ExportEvent.ExportClicked) },
            isLoading = state.isExporting || state.isLoadingPeriods,
            enabled = state.canExport,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun exportHeader(): List<String> = listOf(
    stringResource(Res.string.export_col_date),
    stringResource(Res.string.export_col_type),
    stringResource(Res.string.export_col_amount),
    stringResource(Res.string.export_col_account),
    stringResource(Res.string.export_col_to_account),
    stringResource(Res.string.export_col_envelope),
    stringResource(Res.string.export_col_to_envelope),
    stringResource(Res.string.export_col_notes),
    stringResource(Res.string.export_col_tags),
    stringResource(Res.string.export_col_author),
)

/** The database's kinds and envelope transfer origins in the user's language; an unknown one is written as it comes. */
private class ExportKindLabels(private val transactions: Map<String, String>, private val origins: Map<String, String>, private val transfer: String) {
    fun labelFor(row: ExportRow): String = when (row.recordType) {
        ExportRecordType.Transaction -> transactions[row.kind] ?: row.kind
        ExportRecordType.AccountTransfer -> transfer
        ExportRecordType.EnvelopeTransfer -> origins[row.kind] ?: row.kind
    }
}

@Composable
private fun exportKindLabels() = ExportKindLabels(
    transactions = mapOf(
        "income" to stringResource(Res.string.movement_kind_income),
        "expense" to stringResource(Res.string.movement_kind_expense),
        "adjustment" to stringResource(Res.string.movement_kind_adjustment),
        "group_transfer_out" to stringResource(Res.string.movement_kind_group_transfer_out),
        "group_transfer_in" to stringResource(Res.string.movement_kind_group_transfer_in),
    ),
    origins = mapOf(
        "manual" to stringResource(Res.string.transfer_origin_manual),
        "target" to stringResource(Res.string.transfer_origin_target),
        "commitment" to stringResource(Res.string.transfer_origin_commitment_plain),
        "cover" to stringResource(Res.string.transfer_origin_cover),
        "release" to stringResource(Res.string.transfer_origin_release_plain),
    ),
    transfer = stringResource(Res.string.movement_kind_transfer),
)
