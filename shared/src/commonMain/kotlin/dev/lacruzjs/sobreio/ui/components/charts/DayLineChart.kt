package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.chart_projected
import dev.lacruzjs.sobreio.generated.resources.chart_today
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * A [BalanceLineChart] of points by day (x = epoch day) with its usual texts: the points up to [todayX] solid, the rest
 * dashed. The zoomed page of the small hero charts uses it, so the big version has axes and a crosshair.
 */
@Composable
fun DayLineChart(
    points: List<LineChartPoint>,
    todayX: Long,
    seriesLabel: String,
    currencyCode: String,
    languageTag: String,
    contentDescription: String,
    height: Dp = ZoomedChartHeight,
) {
    BalanceLineChart(
        past = points.filter { it.x <= todayX },
        projection = points.filter { it.x >= todayX },
        todayX = todayX,
        texts = LineChartTexts(
            formatY = { compactAmount(it, languageTag) },
            formatXAxis = { formatDayMonth(LocalDate.fromEpochDays(it), languageTag) },
            formatXTooltip = { formatDate(LocalDate.fromEpochDays(it), languageTag) },
            formatValue = { formatMoney(it, currencyCode, languageTag) },
            todayLabel = stringResource(Res.string.chart_today),
            projectedLabel = stringResource(Res.string.chart_projected),
            seriesLabel = seriesLabel,
        ),
        contentDescription = contentDescription,
        height = height,
    )
}
