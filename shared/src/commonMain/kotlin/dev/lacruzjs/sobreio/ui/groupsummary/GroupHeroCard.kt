// The Group tab's hero card: really free today, its projection chart and the figure that explains it.

package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.generated.resources.info_free_today_3
import dev.lacruzjs.sobreio.generated.resources.info_free_today_2
import dev.lacruzjs.sobreio.generated.resources.info_free_today_1
import dev.lacruzjs.sobreio.ui.components.HeroLabelWithInfo
import dev.lacruzjs.sobreio.generated.resources.chart_enlarge
import dev.lacruzjs.sobreio.generated.resources.chart_table_free
import dev.lacruzjs.sobreio.ui.components.charts.DayLineChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_hero_chart_description
import dev.lacruzjs.sobreio.generated.resources.group_hero_drops
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_commitments
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_contributions
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_income
import dev.lacruzjs.sobreio.generated.resources.group_hero_days_left
import dev.lacruzjs.sobreio.generated.resources.group_hero_last_day
import dev.lacruzjs.sobreio.generated.resources.group_hero_projected_on
import dev.lacruzjs.sobreio.generated.resources.group_summary_free_today
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.HeroCard
import dev.lacruzjs.sobreio.ui.components.HeroFigure
import dev.lacruzjs.sobreio.ui.components.HeroLabel
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.charts.FreeProjectionChart
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The hero card: the really free money today, its projection to the end of the period, the projected figure with the
 * database figure that explains the difference, the lowest day when it goes below zero and how far into the period we
 * are. The big figure is always white; when it is negative a "Below zero" chip says so (`HeroFigure`).
 */
@Composable
internal fun GroupHero(state: GroupSummaryUiState, outlook: GroupOutlook, languageTag: String) {
    val hero = LocalHeroColors.current
    val paper = hero.onContainer
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroLabelWithInfo(
                text = stringResource(Res.string.group_summary_free_today),
                infoTitle = stringResource(Res.string.group_summary_free_today),
                paragraphs = listOf(Res.string.info_free_today_1, Res.string.info_free_today_2, Res.string.info_free_today_3),
                modifier = Modifier.weight(1f),
            )
            state.daysLeft?.let { left ->
                HeroLabel(
                    text = if (left == 0) {
                        stringResource(Res.string.group_hero_last_day)
                    } else {
                        pluralStringResource(Res.plurals.group_hero_days_left, left, left)
                    },
                )
            }
        }
        HeroFigure(amountCents = outlook.freeTodayCents, currencyCode = state.currencyCode)
        if (state.freeProjection.isNotEmpty() && state.today != null) {
            val points = remember(state.freeProjection) { state.freeProjection.map { LineChartPoint(it.day.toEpochDays(), it.freeCents) } }
            val first = state.freeProjection.first()
            val last = state.freeProjection.last()
            val description = stringResource(
                    Res.string.group_hero_chart_description,
                    formatDayMonth(first.day, languageTag),
                    formatDayMonth(last.day, languageTag),
                    formatMoney(first.freeCents, state.currencyCode, languageTag),
                    formatMoney(last.freeCents, state.currencyCode, languageTag),
                )
            val today = state.today
            // A tap opens the chart full screen: axes, crosshair and every day's figure
            val zoom = rememberChartZoom(ZOOM_GROUP_FREE, stringResource(Res.string.group_summary_free_today)) {
                SoftTile {
                    DayLineChart(points, today.toEpochDays(), stringResource(Res.string.chart_table_free), state.currencyCode, languageTag, description)
                }
                ChartTable(
                    columns = listOf(stringResource(Res.string.chart_table_free)),
                    rows = state.freeProjection.map { ChartTableRow(formatDate(it.day, languageTag), listOf(it.freeCents)) },
                    currencyCode = state.currencyCode,
                    role = MoneyRole.Balance,
                )
            }
            ZoomableChart(
                ZOOM_GROUP_FREE,
                Modifier.clickable(onClickLabel = stringResource(Res.string.chart_enlarge), role = Role.Button, onClick = zoom),
            ) {
                FreeProjectionChart(
                    points = points,
                    todayX = today.toEpochDays(),
                    lineColor = paper,
                    negativeColor = hero.negativeOnContainer,
                    ringColor = hero.container,
                    height = 56.dp,
                    contentDescription = description,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                HeroLabel(text = stringResource(Res.string.group_hero_projected_on, formatDayMonth(outlook.periodEnd, languageTag)))
                MoneyText(
                    amountCents = outlook.freeProjectedCents,
                    currencyCode = state.currencyCode,
                    role = MoneyRole.Plain,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    ink = paper,
                )
            }
            state.projectionNote?.let { note ->
                Column(horizontalAlignment = Alignment.End) {
                    HeroLabel(
                        text = stringResource(
                            when (note.kind) {
                                ProjectionNoteKind.Commitments -> Res.string.group_hero_note_commitments
                                ProjectionNoteKind.Contributions -> Res.string.group_hero_note_contributions
                                ProjectionNoteKind.Income -> Res.string.group_hero_note_income
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MoneyText(
                        amountCents = note.cents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Plain,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        ink = paper,
                    )
                }
            }
        }
        state.lowestPoint?.let { lowest ->
            Text(
                text = stringResource(
                    Res.string.group_hero_drops,
                    formatMoney(lowest.freeCents, state.currencyCode, languageTag),
                    formatDayMonth(lowest.day, languageTag),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = hero.negativeOnContainer,
            )
        }
    }
}

private const val ZOOM_GROUP_FREE = "group-free"
