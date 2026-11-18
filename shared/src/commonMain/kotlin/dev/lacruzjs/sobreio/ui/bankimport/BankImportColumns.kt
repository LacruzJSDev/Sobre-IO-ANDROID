package dev.lacruzjs.sobreio.ui.bankimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.bankimport.AmountMode
import dev.lacruzjs.sobreio.domain.bankimport.DateOrder
import dev.lacruzjs.sobreio.domain.bankimport.DecimalMark
import dev.lacruzjs.sobreio.domain.bankimport.StatementTable
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.bank_import_amount_charge_credit
import dev.lacruzjs.sobreio.generated.resources.bank_import_amount_one_column
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_amount
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_amount_mode
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_balance
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_charge
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_credit
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_date
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_description
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_n
import dev.lacruzjs.sobreio.generated.resources.bank_import_column_none
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_remembered
import dev.lacruzjs.sobreio.generated.resources.bank_import_columns_section
import dev.lacruzjs.sobreio.generated.resources.bank_import_continue
import dev.lacruzjs.sobreio.generated.resources.bank_import_date_dmy
import dev.lacruzjs.sobreio.generated.resources.bank_import_date_mdy
import dev.lacruzjs.sobreio.generated.resources.bank_import_date_order
import dev.lacruzjs.sobreio.generated.resources.bank_import_date_ymd
import dev.lacruzjs.sobreio.generated.resources.bank_import_decimal_comma
import dev.lacruzjs.sobreio.generated.resources.bank_import_decimal_dot
import dev.lacruzjs.sobreio.generated.resources.bank_import_decimal_mark
import dev.lacruzjs.sobreio.generated.resources.bank_import_format_section
import dev.lacruzjs.sobreio.generated.resources.bank_import_preview_title
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.components.ChipFormTile
import dev.lacruzjs.sobreio.ui.components.ExpandableFormTile
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** How many statement rows the preview shows under each column name. */
private const val PREVIEW_ROWS = 2

/**
 * The Columns step, only for a format the app does not know: a preview of the file and which column holds the date,
 * the description and the amount (one column, or charge and credit), plus an optional balance. Day order and decimal
 * mark are asked only when the values fit two readings. The choice is remembered for this account.
 */
@Composable
internal fun ColumnsStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val table = state.table ?: return
    val columns = state.columns ?: return
    val mapping = columns.mapping
    StepColumn(
        bottom = {
            PrimaryButton(
                text = stringResource(Res.string.bank_import_continue),
                onClick = { onEvent(BankImportEvent.ColumnsContinueClicked) },
                isLoading = state.isWorking,
            )
        },
    ) {
        FormSection(title = stringResource(Res.string.bank_import_preview_title, state.fileName)) {
            SoftTile {
                (0 until table.columnCount).forEach { c ->
                    val samples = table.body(mapping.headerRow).take(PREVIEW_ROWS).mapNotNull { it.getOrNull(c)?.takeIf(String::isNotBlank) }
                    PreviewColumn(name = columnName(table, mapping.headerRow, c), samples = samples.joinToString(" · "))
                }
            }
        }
        FormSection(title = stringResource(Res.string.bank_import_columns_section)) {
            ColumnTileRow(ColumnTile.Date, Res.string.bank_import_column_date, mapping.dateColumn, table, mapping.headerRow, columns, onEvent)
            ColumnTileRow(ColumnTile.Description, Res.string.bank_import_column_description, mapping.descriptionColumn, table, mapping.headerRow, columns, onEvent)
            ChipFormTile(label = stringResource(Res.string.bank_import_column_amount_mode)) {
                ChipRow {
                    FormChip(stringResource(Res.string.bank_import_amount_one_column), mapping.amountMode == AmountMode.OneColumn, true) {
                        onEvent(BankImportEvent.AmountModeChosen(AmountMode.OneColumn))
                    }
                    FormChip(stringResource(Res.string.bank_import_amount_charge_credit), mapping.amountMode == AmountMode.ChargeAndCredit, true) {
                        onEvent(BankImportEvent.AmountModeChosen(AmountMode.ChargeAndCredit))
                    }
                }
            }
            if (mapping.amountMode == AmountMode.OneColumn) {
                ColumnTileRow(ColumnTile.Amount, Res.string.bank_import_column_amount, mapping.amountColumn, table, mapping.headerRow, columns, onEvent)
            } else {
                ColumnTileRow(ColumnTile.Charge, Res.string.bank_import_column_charge, mapping.chargeColumn, table, mapping.headerRow, columns, onEvent)
                ColumnTileRow(ColumnTile.Credit, Res.string.bank_import_column_credit, mapping.creditColumn, table, mapping.headerRow, columns, onEvent)
            }
            ColumnTileRow(ColumnTile.Balance, Res.string.bank_import_column_balance, mapping.balanceColumn, table, mapping.headerRow, columns, onEvent, optional = true)
        }
        if (columns.askDateOrder || columns.askDecimalMark) {
            FormSection(title = stringResource(Res.string.bank_import_format_section)) {
                if (columns.askDateOrder) {
                    ChipFormTile(label = stringResource(Res.string.bank_import_date_order)) {
                        ChipRow {
                            listOf(
                                DateOrder.DayMonthYear to Res.string.bank_import_date_dmy,
                                DateOrder.MonthDayYear to Res.string.bank_import_date_mdy,
                                DateOrder.YearMonthDay to Res.string.bank_import_date_ymd,
                            ).forEach { (order, label) ->
                                FormChip(stringResource(label), mapping.dateOrder == order, true) { onEvent(BankImportEvent.DateOrderChosen(order)) }
                            }
                        }
                    }
                }
                if (columns.askDecimalMark) {
                    ChipFormTile(label = stringResource(Res.string.bank_import_decimal_mark)) {
                        ChipRow {
                            FormChip(stringResource(Res.string.bank_import_decimal_comma), mapping.decimalMark == DecimalMark.Comma, true) {
                                onEvent(BankImportEvent.DecimalMarkChosen(DecimalMark.Comma))
                            }
                            FormChip(stringResource(Res.string.bank_import_decimal_dot), mapping.decimalMark == DecimalMark.Dot, true) {
                                onEvent(BankImportEvent.DecimalMarkChosen(DecimalMark.Dot))
                            }
                        }
                    }
                }
            }
        }
        columns.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        FormNote(stringResource(Res.string.bank_import_columns_remembered))
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun columnName(table: StatementTable, headerRow: Int, column: Int): String =
    table.header(headerRow).getOrNull(column)?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.bank_import_column_n, column + 1)

/** An expandable tile whose content is one chip per column of the file (plus "None" for an optional one). */
@Composable
private fun ColumnTileRow(
    tile: ColumnTile,
    label: StringResource,
    selected: Int,
    table: StatementTable,
    headerRow: Int,
    columns: ColumnsState,
    onEvent: (BankImportEvent) -> Unit,
    optional: Boolean = false,
) {
    val none = stringResource(Res.string.bank_import_column_none)
    ExpandableFormTile(
        label = stringResource(label),
        expanded = columns.openTile == tile,
        onToggle = { onEvent(BankImportEvent.ColumnTileToggled(tile)) },
        preview = {
            FormTileValue(if (selected >= 0) columnName(table, headerRow, selected) else none)
        },
    ) {
        ChipRow {
            if (optional) FormChip(none, selected < 0, true) { onEvent(BankImportEvent.ColumnChosen(tile, -1)) }
            (0 until table.columnCount).forEach { c ->
                FormChip(columnName(table, headerRow, c), selected == c, true) { onEvent(BankImportEvent.ColumnChosen(tile, c)) }
            }
        }
    }
}

/**
 * One column of the file: its name above a line of sample values. Stacked, so a long description never squeezes the
 * name (side by side the name broke over two lines); the samples are cut with an ellipsis.
 */
@Composable
private fun PreviewColumn(name: String, samples: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(
            samples,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
