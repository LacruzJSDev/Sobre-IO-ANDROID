package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import dev.lacruzjs.sobreio.ui.components.closeOnOutsidePress
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A series of a column chart: its [name] (legend and tooltip) and [color]. */
data class ColumnSeries(val name: String, val color: Color)

/** One group of columns: [label] under it and one value (cents, not negative) per series, in the series order. */
data class ColumnGroup(val label: String, val values: List<Long>)

/**
 * Grouped column chart drawn with Canvas: one group per category (a period), one column per series. Columns are at most
 * 24 dp wide with a 2 dp gap inside a group, rounded 4 dp at the free end and square on the baseline. A legend sits above
 * the plot when there are several series. Touching a group (anywhere in its slot, the whole height of the chart) shows
 * a tooltip with every value. [formatY] writes axis labels (compact) and [formatValue] the full amounts of the tooltip.
 */
@Composable
fun GroupedColumnChart(
    series: List<ColumnSeries>,
    groups: List<ColumnGroup>,
    formatY: (Long) -> String,
    formatValue: (Long) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val bandColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var size by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember(groups) { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (series.size > 1) ChartLegend(series.map { LegendItem(it.name, it.color) })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
                .semantics { this.contentDescription = contentDescription }
                // A press anywhere outside the chart clears the selection (app-wide outside-press host)
                .closeOnOutsidePress(selected != null) { selected = null }
                .onSizeChanged { size = it },
        ) {
            if (groups.isEmpty() || size == IntSize.Zero) return@Box
            val maxValue = groups.maxOf { group -> group.values.maxOrNull() ?: 0L }
            val ticks = remember(groups) { niceTicks(0, maxValue) }
            val yLabels = ticks.values.map { measurer.measure(formatY(it), labelStyle) }
            val labelWidth = yLabels.maxOf { it.size.width }
            val plot = with(density) {
                PlotArea(
                    left = labelWidth + 8.dp.toPx(),
                    top = 8.dp.toPx(),
                    right = size.width - 8.dp.toPx(),
                    bottom = size.height - 24.dp.toPx(),
                )
            }
            val yScale = LinearScale(ticks.min.toDouble(), ticks.max.toDouble(), plot.bottom, plot.top)
            val slotWidth = plot.width / groups.size

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .chartTouch(key = groups, onScrollAway = { selected = null }) { touchX ->
                        selected = slotAt(touchX - plot.left, plot.width, groups.size)
                    },
            ) {
                drawGrid(plot, ticks, yScale, yLabels, gridColor, gridColor)
                groups.forEachIndexed { index, group ->
                    val slotLeft = plot.left + slotWidth * index
                    if (index == selected) {
                        drawRect(bandColor, Offset(slotLeft, plot.top), Size(slotWidth, plot.height))
                    }
                    val layout = columnLayout(slotWidth, series.size, 24.dp.toPx(), 2.dp.toPx())
                    series.forEachIndexed { s, serie ->
                        val value = group.values.getOrElse(s) { 0L }
                        if (value <= 0L) return@forEachIndexed
                        val left = slotLeft + layout.groupStart + s * (layout.barWidth + layout.gap)
                        val top = min(yScale.map(value.toDouble()), plot.bottom - 1.5.dp.toPx())
                        val radius = min(4.dp.toPx(), min(layout.barWidth / 2f, plot.bottom - top))
                        val corner = CornerRadius(radius)
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = left,
                                    top = top,
                                    right = left + layout.barWidth,
                                    bottom = plot.bottom,
                                    topLeftCornerRadius = corner,
                                    topRightCornerRadius = corner,
                                    bottomLeftCornerRadius = CornerRadius.Zero,
                                    bottomRightCornerRadius = CornerRadius.Zero,
                                ),
                            )
                        }
                        drawPath(path, serie.color)
                    }
                    val label = measurer.measure(group.label, labelStyle)
                    drawText(label, topLeft = Offset(slotLeft + (slotWidth - label.size.width) / 2f, plot.bottom + 6.dp.toPx()))
                }
            }

            selected?.let { index ->
                val group = groups[index]
                val tallest = group.values.maxOrNull() ?: 0L
                val centerX = plot.left + slotWidth * (index + 0.5f)
                val barTop = yScale.map(tallest.toDouble())
                val x = (centerX - tooltipSize.width / 2f).coerceIn(0f, max(0f, (size.width - tooltipSize.width).toFloat()))
                val y = (barTop - tooltipSize.height - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
                ChartTooltip(
                    title = group.label,
                    rows = series.mapIndexed { s, serie ->
                        TooltipRow(label = serie.name, value = formatValue(group.values.getOrElse(s) { 0L }), color = serie.color)
                    },
                    modifier = Modifier
                        .onSizeChanged { tooltipSize = it }
                        .offset { IntOffset(x.roundToInt(), y.roundToInt()) },
                )
            }
        }
    }
}

private val CHART_HEIGHT = 200.dp

/** Where the columns of one group sit inside its slot: [groupStart] from the slot's left edge. */
internal class ColumnLayout(val barWidth: Float, val gap: Float, val groupStart: Float)

/**
 * Columns of at most [maxBar] pixels with [gap] between those of a group, filling at most 70 % of the [slot] so
 * neighbouring groups stay apart, and the group centred in it.
 */
internal fun columnLayout(slot: Float, seriesCount: Int, maxBar: Float, gap: Float): ColumnLayout {
    val count = max(1, seriesCount)
    val available = slot * 0.7f - gap * (count - 1)
    val bar = max(1f, min(maxBar, available / count))
    val total = bar * count + gap * (count - 1)
    return ColumnLayout(barWidth = bar, gap = gap, groupStart = (slot - total) / 2f)
}
