package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.generated.resources.info_forecast_2
import dev.lacruzjs.sobreio.generated.resources.info_forecast_1
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.ui.components.charts.FlowsChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
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
import dev.lacruzjs.sobreio.generated.resources.chart_flows_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_period
import dev.lacruzjs.sobreio.generated.resources.chart_legend_in
import dev.lacruzjs.sobreio.generated.resources.chart_legend_out
import dev.lacruzjs.sobreio.generated.resources.detail_section_balance
import dev.lacruzjs.sobreio.generated.resources.detail_section_flows
import dev.lacruzjs.sobreio.generated.resources.detail_section_forecast
import dev.lacruzjs.sobreio.generated.resources.forecast_projected_end
import dev.lacruzjs.sobreio.generated.resources.forecast_scheduled_in
import dev.lacruzjs.sobreio.generated.resources.forecast_scheduled_out
import dev.lacruzjs.sobreio.generated.resources.forecast_shortfall
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.row_subtitle_three
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.TwoColumnLayout
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.MoneyText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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
        info = listOf(Res.string.info_forecast_1, Res.string.info_forecast_2),
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

private const val ZOOM_FLOWS = "account-flows"
