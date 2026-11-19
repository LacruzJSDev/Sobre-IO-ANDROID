package dev.lacruzjs.sobreio.ui.accountdetail


import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.generated.resources.chart_table_balance
import dev.lacruzjs.sobreio.ui.components.charts.FlowsChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.BalanceChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import dev.lacruzjs.sobreio.ui.components.BalanceOnDay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_compare
import dev.lacruzjs.sobreio.generated.resources.bank_import_no_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import_value
import dev.lacruzjs.sobreio.generated.resources.bank_import_last_import
import dev.lacruzjs.sobreio.generated.resources.bank_import_section_bank
import dev.lacruzjs.sobreio.generated.resources.accounts_in_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.accounts_outside_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.action_see_all_movements
import dev.lacruzjs.sobreio.generated.resources.ic_list
import dev.lacruzjs.sobreio.generated.resources.chart_balance_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_period
import dev.lacruzjs.sobreio.generated.resources.chart_legend_in
import dev.lacruzjs.sobreio.generated.resources.chart_legend_out
import dev.lacruzjs.sobreio.generated.resources.chart_no_data
import dev.lacruzjs.sobreio.generated.resources.chart_projected
import dev.lacruzjs.sobreio.generated.resources.chart_today
import dev.lacruzjs.sobreio.generated.resources.detail_no_movements
import dev.lacruzjs.sobreio.generated.resources.detail_section_balance
import dev.lacruzjs.sobreio.generated.resources.detail_section_balance_chart
import dev.lacruzjs.sobreio.generated.resources.detail_section_flows
import dev.lacruzjs.sobreio.generated.resources.detail_section_forecast
import dev.lacruzjs.sobreio.generated.resources.detail_section_latest
import dev.lacruzjs.sobreio.generated.resources.detail_section_upcoming
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.forecast_projected_end
import dev.lacruzjs.sobreio.generated.resources.forecast_scheduled_in
import dev.lacruzjs.sobreio.generated.resources.forecast_scheduled_out
import dev.lacruzjs.sobreio.generated.resources.forecast_shortfall
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.generated.resources.movement_from_account
import dev.lacruzjs.sobreio.generated.resources.movement_to_account
import dev.lacruzjs.sobreio.generated.resources.range_1m
import dev.lacruzjs.sobreio.generated.resources.range_1y
import dev.lacruzjs.sobreio.generated.resources.range_3m
import dev.lacruzjs.sobreio.generated.resources.range_6m
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.row_subtitle_three
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.TwoColumnLayout
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.charts.BalanceLineChart
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import dev.lacruzjs.sobreio.ui.components.charts.LineChartTexts
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.MoneyText
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import dev.lacruzjs.sobreio.ui.components.IconActionButton

/**
 * The Summary tab: balance, balance chart with its projection, forecast to the end of the period, in and out by period,
 * next due items, latest movements and the comparison with the bank. One scrolling column of titled sections whose
 * content sits on soft tiles, with spacing and no dividers.
 */
@Composable
internal fun AccountSummaryTab(
    state: AccountDetailUiState,
    onEvent: (AccountDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = state.account ?: return
    val languageTag = Locale.current.toLanguageTag()
    // The account itself on the left; what happens around it on the right, each with its own scroll on a wide pane
    TwoColumnLayout(
        modifier = modifier,
        spacing = 12.dp,
        top = { ErrorText(state.actionError, modifier = Modifier.fillMaxWidth()) },
        left = {
            BalanceSection(account, state, languageTag)
            BalanceOnDay(
                day = state.balanceDay,
                balanceCents = state.balanceOnDayCents,
                currencyCode = state.currencyCode,
                today = state.today,
                onDaySelected = { onEvent(AccountDetailEvent.BalanceDaySelected(it)) },
            )
            BalanceChartSection(state, languageTag, onEvent)
            state.forecast?.let { ForecastSection(state, it.scheduledInCents, it.scheduledOutCents, it.projectedEndCents, it.shortfallCents, languageTag) }
        },
        right = {
            if (state.flows.isNotEmpty()) FlowsSection(state, languageTag)
            if (state.upcoming.isNotEmpty()) UpcomingSection(state, languageTag)
            LatestMovementsSection(state, languageTag, onEvent)
            BankSection(state, languageTag, onEvent)
        },
    )
}

/**
 * Everything about the bank in one place (D-52): when the account was last imported, and the two actions that check it
 * against the bank, compare the balance and import a statement.
 */
@Composable
private fun BankSection(state: AccountDetailUiState, languageTag: String, onEvent: (AccountDetailEvent) -> Unit) {
    FormSection(title = stringResource(Res.string.bank_import_section_bank)) {
        SoftTile {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.bank_import_last_import),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val last = state.lastImport
                Text(
                    text = if (last?.createdDay != null) {
                        pluralStringResource(
                            Res.plurals.bank_import_last_import_value,
                            last.rowsTotal,
                            formatShortDate(last.createdDay, languageTag, state.today?.year ?: 0),
                            last.rowsTotal,
                        )
                    } else {
                        stringResource(Res.string.bank_import_no_last_import)
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                text = stringResource(Res.string.bank_import_action_compare),
                onClick = { onEvent(AccountDetailEvent.CompareClicked) },
                modifier = Modifier.weight(1f),
            )
            if (state.canImport) {
                SecondaryButton(
                    text = stringResource(Res.string.bank_import_action_import),
                    onClick = { onEvent(AccountDetailEvent.BankImportClicked) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Soft rounded surface the content of a section sits on. */
@Composable
internal fun SoftTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun BalanceSection(account: Account, state: AccountDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.detail_section_balance)) {
        MoneyText(
            amountCents = account.balanceCents,
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val kind = stringResource(account.kind.labelRes())
        val scope = stringResource(
            if (account.inEnvelopes) Res.string.accounts_in_envelopes_title else Res.string.accounts_outside_envelopes_title,
        )
        val subtitle = if (state.hasSeveralMembers) {
            stringResource(
                Res.string.row_subtitle_three,
                kind,
                state.holderName ?: stringResource(Res.string.holder_joint),
                scope,
            )
        } else {
            stringResource(Res.string.row_subtitle, kind, scope)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun BalanceChartSection(state: AccountDetailUiState, languageTag: String, onEvent: (AccountDetailEvent) -> Unit) {
    val title = stringResource(Res.string.detail_section_balance_chart)
    // The zoomed page: the same range chips and chart, taller, and every point of the series as a figure
    val zoom = rememberChartZoom(ZOOM_BALANCE, title) {
        RangeChips(state, onEvent)
        SoftTile { BalanceChart(state, languageTag, ZoomedChartHeight) }
        ChartTable(
            columns = listOf(stringResource(Res.string.chart_table_balance)),
            rows = state.balanceSeries.asReversed().map { ChartTableRow(formatDate(it.day, languageTag), listOf(it.balanceCents)) },
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
        )
    }
    FormSection(title = title, action = { ChartZoomButton(zoom) }) {
        RangeChips(state, onEvent)
        ZoomableChart(ZOOM_BALANCE) { SoftTile { BalanceChart(state, languageTag) } }
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

private fun BalanceRange.labelRes() = when (this) {
    BalanceRange.OneMonth -> Res.string.range_1m
    BalanceRange.ThreeMonths -> Res.string.range_3m
    BalanceRange.SixMonths -> Res.string.range_6m
    BalanceRange.OneYear -> Res.string.range_1y
}

@Composable
private fun ForecastSection(
    state: AccountDetailUiState,
    scheduledIn: Long,
    scheduledOut: Long,
    projectedEnd: Long,
    shortfall: Long,
    languageTag: String,
) {
    val date = state.shortfallDate ?: state.periodEnd ?: state.today
    StatCard(
        title = stringResource(Res.string.detail_section_forecast),
        currencyCode = state.currencyCode,
        rows = listOf(
            StatRow(stringResource(Res.string.forecast_scheduled_in), scheduledIn, MoneyRole.Flow),
            StatRow(stringResource(Res.string.forecast_scheduled_out), -scheduledOut, MoneyRole.Flow),
            StatRow(stringResource(Res.string.forecast_projected_end), projectedEnd, MoneyRole.Balance, emphasised = true),
        ),
        warning = if (shortfall > 0) {
            stringResource(
                Res.string.forecast_shortfall,
                formatMoney(shortfall, state.currencyCode, languageTag),
                date?.let { formatDayMonth(it, languageTag) }.orEmpty(),
            )
        } else {
            null
        },
    )
}

@Composable
private fun FlowsSection(state: AccountDetailUiState, languageTag: String) {
    val title = stringResource(Res.string.detail_section_flows)
    val zoom = rememberChartZoom(ZOOM_FLOWS, title) {
        SoftTile { FlowsChart(state, languageTag, ZoomedChartHeight) }
        ChartTable(
            columns = listOf(stringResource(Res.string.chart_legend_in), stringResource(Res.string.chart_legend_out)),
            rows = state.flows.asReversed().map {
                ChartTableRow(formatMonthShort(it.startDate, languageTag), listOf(it.inflowCents, -it.outflowCents))
            },
            currencyCode = state.currencyCode,
            role = MoneyRole.Flow,
        )
    }
    FormSection(title = title, action = { ChartZoomButton(zoom) }) {
        ZoomableChart(ZOOM_FLOWS) { SoftTile { FlowsChart(state, languageTag) } }
    }
}

@Composable
private fun FlowsChart(state: AccountDetailUiState, languageTag: String, height: Dp = FlowsChartHeight) {
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

private const val ZOOM_BALANCE = "account-balance"
private const val ZOOM_FLOWS = "account-flows"

@Composable
private fun UpcomingSection(state: AccountDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.detail_section_upcoming)) {
        SoftTile {
            state.upcoming.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = formatDate(item.dueDate, languageTag),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyText(
                        amountCents = item.amountCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Flow,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestMovementsSection(state: AccountDetailUiState, languageTag: String, onEvent: (AccountDetailEvent) -> Unit) {
    FormSection(
        title = stringResource(Res.string.detail_section_latest),
        action = {
            IconActionButton(
                icon = vectorResource(Res.drawable.ic_list),
                iconSize = 24.dp,
                label = stringResource(Res.string.action_see_all_movements),
                onClick = { onEvent(AccountDetailEvent.SeeAllClicked) },
            )
        },
    ) {
        SoftTile {
            if (state.movements.isEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_no_movements),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.movements.forEach { MovementRow(it, state.currencyCode, languageTag) }
            }
        }
    }
}

@Composable
private fun MovementRow(row: MovementRowState, currencyCode: String, languageTag: String) {
    val movement = row.movement
    val primary = movement.notes ?: stringResource(movement.kind.labelRes())
    val secondary = when {
        row.counterAccountName != null && movement.kind == MovementKind.TransferOut ->
            stringResource(Res.string.movement_to_account, row.counterAccountName)
        row.counterAccountName != null && movement.kind == MovementKind.TransferIn ->
            stringResource(Res.string.movement_from_account, row.counterAccountName)
        row.envelopeIsUnassigned -> stringResource(Res.string.envelopes_unassigned)
        else -> row.envelopeName
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EntityAvatar(name = row.authorName.orEmpty(), icon = null, color = null, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                amountCents = movement.amountCents,
                currencyCode = currencyCode,
                role = MoneyRole.Flow,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                text = formatDayMonth(movement.date, languageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
