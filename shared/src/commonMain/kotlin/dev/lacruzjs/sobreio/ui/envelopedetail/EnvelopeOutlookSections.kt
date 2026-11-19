// Envelope detail: the zoomable "Really free" card, balance on a day and the chart by period.

package dev.lacruzjs.sobreio.ui.envelopedetail

import dev.lacruzjs.sobreio.generated.resources.envelope_outlook_more
import dev.lacruzjs.sobreio.generated.resources.info_envelope_free_2
import dev.lacruzjs.sobreio.generated.resources.info_envelope_free_1
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_left
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_activity
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.ui.components.charts.FlowsChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_free_until
import dev.lacruzjs.sobreio.generated.resources.outlook_until_note
import dev.lacruzjs.sobreio.generated.resources.outlook_until_period_end
import dev.lacruzjs.sobreio.generated.resources.outlook_until_label
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import dev.lacruzjs.sobreio.ui.components.BalanceOnDay
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.chart_legend_assigned
import dev.lacruzjs.sobreio.generated.resources.chart_legend_spent
import dev.lacruzjs.sobreio.generated.resources.envelope_chart_description
import dev.lacruzjs.sobreio.generated.resources.envelope_chart_period
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_activity
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_committed
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_free
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_left
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_chart
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_free
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_shortfall
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** What is left once the commitments to the end of the period are paid (`envelope_outlook_current`). */
/**
 * "Really free" on the detail is just the card; a tap grows it into a page with the tools to explore it: the same
 * figures up to a chosen day ("Look until") and the balance on a past day.
 */
@Composable
internal fun ZoomableOutlook(
    state: EnvelopeDetailUiState,
    periodOutlook: EnvelopeOutlook,
    languageTag: String,
    onEvent: (EnvelopeDetailEvent) -> Unit,
) {
    val title = stringResource(Res.string.envelope_detail_section_free)
    val open = rememberChartZoom(ZOOM_OUTLOOK, title) {
        OutlookSection(state.outlookUntilFigures ?: periodOutlook, state.outlookUntil, state.currencyCode, languageTag)
        DateFormTile(
            label = stringResource(Res.string.outlook_until_label),
            date = state.outlookUntil,
            onDateSelected = { onEvent(EnvelopeDetailEvent.OutlookUntilSelected(it)) },
            emptyText = stringResource(Res.string.outlook_until_period_end),
            onClear = if (state.outlookUntil != null) ({ onEvent(EnvelopeDetailEvent.OutlookUntilCleared) }) else null,
            note = stringResource(Res.string.outlook_until_note),
        )
        EnvelopeBalanceOnDay(state, onEvent)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ZoomableChart(
            ZOOM_OUTLOOK,
            Modifier.clickable(onClickLabel = stringResource(Res.string.envelope_outlook_more), role = Role.Button, onClick = open),
        ) {
            OutlookSection(state.outlookUntilFigures ?: periodOutlook, state.outlookUntil, state.currencyCode, languageTag)
        }
        Text(
            text = stringResource(Res.string.envelope_outlook_more),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
internal fun EnvelopeBalanceOnDay(state: EnvelopeDetailUiState, onEvent: (EnvelopeDetailEvent) -> Unit) {
    BalanceOnDay(
        day = state.balanceDay,
        balanceCents = state.balanceOnDayCents,
        currencyCode = state.currencyCode,
        today = state.today,
        onDaySelected = { onEvent(EnvelopeDetailEvent.BalanceDaySelected(it)) },
    )
}

private const val ZOOM_OUTLOOK = "envelope-outlook"

@Composable
private fun OutlookSection(outlook: EnvelopeOutlook, until: LocalDate?, currencyCode: String, languageTag: String) {
    StatCard(
        title = if (until == null) {
            stringResource(Res.string.envelope_detail_section_free)
        } else {
            stringResource(Res.string.envelope_detail_section_free_until, formatDate(until, languageTag))
        },
        info = listOf(Res.string.info_envelope_free_1, Res.string.info_envelope_free_2),
        currencyCode = currencyCode,
        rows = listOf(
            StatRow(stringResource(Res.string.envelope_detail_committed), -outlook.committedCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_free), outlook.freeCents, MoneyRole.Balance, emphasised = true),
        ),
        warning = if (outlook.shortfallCents > 0) {
            stringResource(Res.string.envelope_detail_shortfall, formatMoney(outlook.shortfallCents, currencyCode, languageTag))
        } else {
            null
        },
    )
}

@Composable
internal fun ChartSection(state: EnvelopeDetailUiState, languageTag: String) {
    val title = stringResource(Res.string.envelope_detail_section_chart)
    // The zoomed page: the chart taller and each period as the database has it (assigned, movements, what was left)
    val zoom = rememberChartZoom(ZOOM_ENVELOPE_CHART, title) {
        SoftTile { EnvelopeChart(state, languageTag, ZoomedChartHeight) }
        ChartTable(
            columns = listOf(
                stringResource(Res.string.chart_legend_assigned),
                stringResource(Res.string.envelope_detail_activity),
                stringResource(Res.string.envelope_detail_left),
            ),
            rows = state.chartPeriods.asReversed().map {
                ChartTableRow(formatMonthShort(it.startDate, languageTag), listOf(it.assignedCents, it.activityCents, it.closingCents))
            },
            currencyCode = state.currencyCode,
            role = MoneyRole.Flow,
        )
    }
    FormSection(title = title, action = { ChartZoomButton(zoom) }) {
        ZoomableChart(ZOOM_ENVELOPE_CHART) { SoftTile { EnvelopeChart(state, languageTag) } }
    }
}

@Composable
private fun EnvelopeChart(state: EnvelopeDetailUiState, languageTag: String, height: Dp = FlowsChartHeight) {
    val groups = state.chartPeriods.map { ColumnGroup(formatMonthShort(it.startDate, languageTag), it.chartColumns()) }
    val periods = state.chartPeriods.map {
        val (assigned, spent) = it.chartColumns()
        stringResource(
            Res.string.envelope_chart_period,
            formatMonthShort(it.startDate, languageTag),
            formatMoney(assigned, state.currencyCode, languageTag),
            formatMoney(spent, state.currencyCode, languageTag),
        )
    }.joinToString(" ")
    GroupedColumnChart(
        series = listOf(
            ColumnSeries(stringResource(Res.string.chart_legend_assigned), LocalMoneyColors.current.inflow),
            ColumnSeries(stringResource(Res.string.chart_legend_spent), LocalMoneyColors.current.outflow),
        ),
        groups = groups,
        formatY = { compactAmount(it, languageTag) },
        formatValue = { formatMoney(it, state.currencyCode, languageTag) },
        contentDescription = stringResource(Res.string.envelope_chart_description, state.chartPeriods.size, periods),
        height = height,
    )
}

private const val ZOOM_ENVELOPE_CHART = "envelope-chart"
