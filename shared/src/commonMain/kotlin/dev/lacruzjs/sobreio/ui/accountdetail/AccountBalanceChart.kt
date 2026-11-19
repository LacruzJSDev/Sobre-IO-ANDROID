// Account detail Summary: the balance chart, its zoomed page and range chips.

package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.generated.resources.account_chart_more_hint
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.generated.resources.chart_table_balance
import dev.lacruzjs.sobreio.ui.components.charts.BalanceChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import dev.lacruzjs.sobreio.ui.components.BalanceOnDay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.chart_balance_description
import dev.lacruzjs.sobreio.generated.resources.chart_no_data
import dev.lacruzjs.sobreio.generated.resources.chart_projected
import dev.lacruzjs.sobreio.generated.resources.chart_today
import dev.lacruzjs.sobreio.generated.resources.detail_section_balance
import dev.lacruzjs.sobreio.generated.resources.detail_section_balance_chart
import dev.lacruzjs.sobreio.generated.resources.range_1m
import dev.lacruzjs.sobreio.generated.resources.range_1y
import dev.lacruzjs.sobreio.generated.resources.range_3m
import dev.lacruzjs.sobreio.generated.resources.range_6m
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.charts.BalanceLineChart
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import dev.lacruzjs.sobreio.ui.components.charts.LineChartTexts
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BalanceChartSection(state: AccountDetailUiState, languageTag: String, onEvent: (AccountDetailEvent) -> Unit) {
    val title = stringResource(Res.string.detail_section_balance_chart)
    // The detail keeps the chart only; exploring it (the range, the balance on a chosen day, every point as a figure) is
    // the zoomed page's job
    val zoom = rememberChartZoom(ZOOM_BALANCE, title) {
        RangeChips(state, onEvent)
        SoftTile { BalanceChart(state, languageTag, ZoomedChartHeight) }
        BalanceOnDay(
            day = state.balanceDay,
            balanceCents = state.balanceOnDayCents,
            currencyCode = state.currencyCode,
            today = state.today,
            onDaySelected = { onEvent(AccountDetailEvent.BalanceDaySelected(it)) },
        )
        ChartTable(
            columns = listOf(stringResource(Res.string.chart_table_balance)),
            rows = state.balanceSeries.asReversed().map { ChartTableRow(formatDate(it.day, languageTag), listOf(it.balanceCents)) },
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
        )
    }
    FormSection(title = title, action = { ChartZoomButton(zoom) }) {
        ZoomableChart(ZOOM_BALANCE) { SoftTile { BalanceChart(state, languageTag) } }
        Text(
            text = stringResource(Res.string.account_chart_more_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeChips(state: AccountDetailUiState, onEvent: (AccountDetailEvent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BalanceRange.entries.forEach { range ->
            FilterChip(
                selected = range == state.range,
                onClick = { onEvent(AccountDetailEvent.RangeSelected(range)) },
                label = { Text(stringResource(range.labelRes())) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun BalanceChart(state: AccountDetailUiState, languageTag: String, height: Dp = BalanceChartHeight) {
    val today = state.today
    if (today == null || state.balanceSeries.size < 2) {
        Text(
            text = stringResource(Res.string.chart_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val past = remember(state.balanceSeries) {
            state.balanceSeries.map { LineChartPoint(it.day.toEpochDays(), it.balanceCents) }
        }
        val projection = remember(state.projection, state.periodEnd) { projectionPoints(state) }
        val first = state.balanceSeries.first()
        val last = state.balanceSeries.last()
        val texts = LineChartTexts(
            formatY = { compactAmount(it, languageTag) },
            formatXAxis = { formatDayMonth(LocalDate.fromEpochDays(it), languageTag) },
            formatXTooltip = { formatDate(LocalDate.fromEpochDays(it), languageTag) },
            formatValue = { formatMoney(it, state.currencyCode, languageTag) },
            todayLabel = stringResource(Res.string.chart_today),
            projectedLabel = stringResource(Res.string.chart_projected),
            seriesLabel = stringResource(Res.string.detail_section_balance),
        )
        BalanceLineChart(
            past = past,
            projection = projection,
            todayX = today.toEpochDays(),
            texts = texts,
            contentDescription = stringResource(
                Res.string.chart_balance_description,
                formatDayMonth(first.day, languageTag),
                formatDayMonth(last.day, languageTag),
                formatMoney(first.balanceCents, state.currencyCode, languageTag),
                formatMoney(last.balanceCents, state.currencyCode, languageTag),
            ),
            // A new range is on its way: the old one stays, faded
            modifier = Modifier.alpha(if (state.isSeriesLoading) 0.5f else 1f),
            height = height,
        )
    }
}

/**
 * The database's projection (one row per day with something due, starting today) as chart points, extended with a last
 * point at the end of the period so the dashed line reaches it: the balance stays what it was after the last due item.
 */
internal fun projectionPoints(state: AccountDetailUiState): List<LineChartPoint> {
    val points = state.projection.map { LineChartPoint(it.day.toEpochDays(), it.balanceCents) }
    val end = state.periodEnd?.toEpochDays() ?: return points
    val last = points.lastOrNull() ?: return points
    return if (end > last.x) points + LineChartPoint(end, last.y) else points
}

internal fun BalanceRange.labelRes() = when (this) {
    BalanceRange.OneMonth -> Res.string.range_1m
    BalanceRange.ThreeMonths -> Res.string.range_3m
    BalanceRange.SixMonths -> Res.string.range_6m
    BalanceRange.OneYear -> Res.string.range_1y
}

private const val ZOOM_BALANCE = "account-balance"
