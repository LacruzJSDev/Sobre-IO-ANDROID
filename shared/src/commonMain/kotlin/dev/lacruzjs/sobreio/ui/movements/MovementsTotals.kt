package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.ui.components.displayName
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_unknown_envelope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.chart_flows_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_period
import dev.lacruzjs.sobreio.generated.resources.chart_legend_in
import dev.lacruzjs.sobreio.generated.resources.chart_legend_out
import dev.lacruzjs.sobreio.generated.resources.movements_report_by_envelope
import dev.lacruzjs.sobreio.generated.resources.movements_report_by_period
import dev.lacruzjs.sobreio.generated.resources.movements_report_loading
import dev.lacruzjs.sobreio.generated.resources.movements_report_open
import dev.lacruzjs.sobreio.generated.resources.movements_report_title
import dev.lacruzjs.sobreio.generated.resources.movements_totals_hint
import dev.lacruzjs.sobreio.generated.resources.movements_totals_in
import dev.lacruzjs.sobreio.generated.resources.movements_totals_net
import dev.lacruzjs.sobreio.generated.resources.movements_totals_out
import dev.lacruzjs.sobreio.generated.resources.movements_totals_title
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Totals and report of what the Movements filters pick (D-54). Every figure is the database's
 * (`search_movements_summary`, `_by_period`, `_by_envelope`); the app only draws them. The card grows into the report page
 * with the chart zoom.
 */

private const val ZOOM_REPORT = "movements-report"

/** "N movements": in, out and the net of the current filter, and "See report". */
@Composable
internal fun TotalsCard(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    val totals = state.totals ?: return
    val count = totals.count.toInt()
    val title = pluralStringResource(Res.plurals.movements_totals_title, count, count)
    val openReport = rememberChartZoom(ZOOM_REPORT, stringResource(Res.string.movements_report_title)) {
        ReportPage(state)
    }
    ZoomableChart(ZOOM_REPORT, Modifier.padding(bottom = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                title = title,
                currencyCode = state.currencyCode,
                rows = listOf(
                    StatRow(stringResource(Res.string.movements_totals_in), totals.inflowCents, MoneyRole.Flow),
                    StatRow(stringResource(Res.string.movements_totals_out), -totals.outflowCents, MoneyRole.Flow),
                    StatRow(stringResource(Res.string.movements_totals_net), totals.netCents, MoneyRole.Balance, emphasised = true),
                ),
            )
            SecondaryButton(
                text = stringResource(Res.string.movements_report_open),
                onClick = {
                    onEvent(MovementsEvent.ReportOpened)
                    openReport()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The left column without a filter: what choosing one gives. */
@Composable
internal fun TotalsHint() {
    Text(
        text = stringResource(Res.string.movements_totals_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** The zoomed page: the totals, in and out per period (chart and table) and what each envelope took or received. */
@Composable
private fun ReportPage(state: MovementsUiState) {
    val languageTag = Locale.current.toLanguageTag()
    val report = state.report
    if (report == null) {
        Text(
            text = stringResource(Res.string.movements_report_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    if (report.periods.isNotEmpty()) {
        FormSection(title = stringResource(Res.string.movements_report_by_period)) {
            SoftTile {
                GroupedColumnChart(
                    series = listOf(
                        ColumnSeries(stringResource(Res.string.chart_legend_in), LocalMoneyColors.current.inflow),
                        ColumnSeries(stringResource(Res.string.chart_legend_out), LocalMoneyColors.current.outflow),
                    ),
                    groups = report.periods.map { ColumnGroup(formatMonthShort(it.start, languageTag), listOf(it.inflowCents, it.outflowCents)) },
                    formatY = { compactAmount(it, languageTag) },
                    formatValue = { formatMoney(it, state.currencyCode, languageTag) },
                    contentDescription = stringResource(
                        Res.string.chart_flows_description,
                        report.periods.size,
                        report.periods.map {
                            stringResource(
                                Res.string.chart_flows_period,
                                formatMonthShort(it.start, languageTag),
                                formatMoney(it.inflowCents, state.currencyCode, languageTag),
                                formatMoney(it.outflowCents, state.currencyCode, languageTag),
                            )
                        }.joinToString(" "),
                    ),
                    height = ZoomedChartHeight,
                )
            }
            ChartTable(
                columns = listOf(stringResource(Res.string.chart_legend_in), stringResource(Res.string.chart_legend_out)),
                rows = report.periods.asReversed().map {
                    ChartTableRow(formatMonthShort(it.start, languageTag), listOf(it.inflowCents, -it.outflowCents))
                },
                currencyCode = state.currencyCode,
                role = MoneyRole.Flow,
            )
        }
    }
    if (report.envelopes.isNotEmpty()) {
        val unknown = stringResource(Res.string.envelope_transfer_unknown_envelope)
        val biggest = report.envelopes.maxOf { maxOf(it.outflowCents, it.inflowCents) }.coerceAtLeast(1)
        FormSection(title = stringResource(Res.string.movements_report_by_envelope)) {
            SoftTile {
                report.envelopes.forEach { row ->
                    val envelope = state.envelope(row.envelopeId)
                    val name = envelope?.displayName() ?: unknown
                    val spent = row.outflowCents >= row.inflowCents
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            MoneyText(
                                amountCents = if (spent) -row.outflowCents else row.inflowCents,
                                currencyCode = state.currencyCode,
                                role = MoneyRole.Flow,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        // Bar length is a display ratio of two database figures, not a sum
                        val fraction = (if (spent) row.outflowCents else row.inflowCents).toFloat() / biggest
                        Box(
                            Modifier
                                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .background(
                                    if (spent) LocalMoneyColors.current.outflow else LocalMoneyColors.current.inflow,
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}
