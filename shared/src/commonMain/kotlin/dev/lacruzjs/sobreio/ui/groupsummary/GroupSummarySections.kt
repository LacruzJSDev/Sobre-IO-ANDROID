// The Group tab's sections: the next movements, where the money went and in and out by period.

package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.ui.components.displayName
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.ui.components.charts.FlowsChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.chart_flows_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_period
import dev.lacruzjs.sobreio.generated.resources.chart_legend_in
import dev.lacruzjs.sobreio.generated.resources.chart_legend_out
import dev.lacruzjs.sobreio.generated.resources.group_next_empty
import dev.lacruzjs.sobreio.generated.resources.group_next_open
import dev.lacruzjs.sobreio.generated.resources.group_next_open_pending
import dev.lacruzjs.sobreio.generated.resources.group_next_title
import dev.lacruzjs.sobreio.generated.resources.ic_list
import dev.lacruzjs.sobreio.domain.model.NEXT_DUE_WINDOW_DAYS
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import org.jetbrains.compose.resources.vectorResource
import dev.lacruzjs.sobreio.generated.resources.group_summary_flows_title
import dev.lacruzjs.sobreio.generated.resources.group_summary_spending_all
import dev.lacruzjs.sobreio.generated.resources.group_summary_spending_title
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

/** Horizontal bars of what each envelope spent this period (top 6); a bar opens the envelope, the link the Envelopes tab. */
@Composable
internal fun SpendingSection(
    state: GroupSummaryUiState,
    onEvent: (GroupSummaryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormSection(title = stringResource(Res.string.group_summary_spending_title), modifier = modifier) {
        SoftTile {
            val bar = LocalMoneyColors.current.outflow
            state.spending.forEach { item ->
                val name = item.envelope.displayName()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEvent(GroupSummaryEvent.SpendingClicked(item.envelope.id)) },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        MoneyText(
                            amountCents = -item.spentCents,
                            currencyCode = state.currencyCode,
                            role = MoneyRole.Flow,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                    // Length relative to the biggest bar: a display ratio
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.background)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.spendingFraction(item)).clip(RoundedCornerShape(50)).background(bar))
                    }
                }
            }
            TextButton(onClick = { onEvent(GroupSummaryEvent.SeeAllEnvelopesClicked) }) {
                Text(stringResource(Res.string.group_summary_spending_all))
            }
        }
    }
}

@Composable
internal fun FlowsSection(
    state: GroupSummaryUiState,
    languageTag: String,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.group_summary_flows_title)
    val zoom = rememberChartZoom(ZOOM_GROUP_FLOWS, title) {
        SoftTile { GroupFlowsChart(state, languageTag, ZoomedChartHeight) }
        ChartTable(
            columns = listOf(stringResource(Res.string.chart_legend_in), stringResource(Res.string.chart_legend_out)),
            rows = state.flows.asReversed().map {
                ChartTableRow(formatMonthShort(it.startDate, languageTag), listOf(it.inflowCents, -it.outflowCents))
            },
            currencyCode = state.currencyCode,
            role = MoneyRole.Flow,
        )
    }
    FormSection(title = title, modifier = modifier, action = { ChartZoomButton(zoom) }, segmented = false) {
        ZoomableChart(ZOOM_GROUP_FLOWS) { SoftTile { GroupFlowsChart(state, languageTag) } }
    }
}

@Composable
private fun GroupFlowsChart(state: GroupSummaryUiState, languageTag: String, height: Dp = FlowsChartHeight) {
    val groups = state.flows.map {
        ColumnGroup(formatMonthShort(it.startDate, languageTag), listOf(it.inflowCents, it.outflowCents))
    }
    val periods = state.flows.map {
        stringResource(
            Res.string.chart_flows_period,
            formatMonthShort(it.startDate, languageTag),
            formatMoney(it.inflowCents, state.currencyCode, languageTag),
            formatMoney(it.outflowCents, state.currencyCode, languageTag),
        )
    }.joinToString(" ")
    GroupedColumnChart(
        series = listOf(
            ColumnSeries(stringResource(Res.string.chart_legend_in), LocalMoneyColors.current.inflow),
            ColumnSeries(stringResource(Res.string.chart_legend_out), LocalMoneyColors.current.outflow),
        ),
        groups = groups,
        formatY = { compactAmount(it, languageTag) },
        formatValue = { formatMoney(it, state.currencyCode, languageTag) },
        contentDescription = stringResource(Res.string.chart_flows_description, state.flows.size, periods),
        height = height,
    )
}

private const val ZOOM_GROUP_FLOWS = "group-flows"
/**
 * "Next movements": the first due dates from tomorrow on, read only, soonest first. The icon at the right of the title opens
 * the review screen, where the ones already due are confirmed or skipped and the next ones can be registered before they are
 * due; its badge counts the ones waiting for review.
 */
@Composable
internal fun NextMovementsSection(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    val reviewLabel = if (state.pendingCount > 0) {
        stringResource(Res.string.group_next_open_pending, state.pendingCount)
    } else {
        stringResource(Res.string.group_next_open)
    }
    FormSection(
        title = stringResource(Res.string.group_next_title),
        action = {
            IconActionButton(
                icon = vectorResource(Res.drawable.ic_list),
                iconSize = 24.dp,
                label = reviewLabel,
                badge = state.pendingCount,
                onClick = { onEvent(GroupSummaryEvent.ReviewClicked) },
            )
        },
    ) {
        SoftTile {
            if (state.upcoming.isEmpty()) {
                Text(
                    text = stringResource(Res.string.group_next_empty, NEXT_DUE_WINDOW_DAYS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.upcoming.forEach { item -> NextMovementRow(item, state.currencyCode, languageTag) }
            }
        }
    }
}

@Composable
private fun NextMovementRow(item: DueOccurrence, currencyCode: String, languageTag: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // No entity colour: a commitment is drawn in ink with the icon of its kind, as in the review screen
        EntityAvatar(name = item.name, icon = item.kind.icon(), color = null, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = formatWeekdayDayMonth(item.dueDate, languageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyText(
            amountCents = item.signedCents,
            currencyCode = currencyCode,
            role = MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
