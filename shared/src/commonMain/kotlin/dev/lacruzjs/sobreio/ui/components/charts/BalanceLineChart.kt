package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** A point of a line chart: [x] is an epoch day and [y] an amount in cents. */
data class LineChartPoint(val x: Long, val y: Long)

/** Everything the chart writes, formatted by the caller (it knows the language and the currency). */
class LineChartTexts(
    /** Axis label of an amount (compact: `1.5K`). */
    val formatY: (Long) -> String,
    /** Axis label of a day (`Nov 14`). */
    val formatXAxis: (Long) -> String,
    /** Title of the tooltip for a day (`Nov 14, 2026`). */
    val formatXTooltip: (Long) -> String,
    /** Full amount for the tooltip (`€1,298.15`). */
    val formatValue: (Long) -> String,
    /** Label of the vertical line at today. */
    val todayLabel: String,
    /** Added to the tooltip row of a day that has not happened yet. */
    val projectedLabel: String,
    /** Name of the series, shown in the tooltip row. */
    val seriesLabel: String,
)

/**
 * Line chart of one balance over time, drawn with Canvas. [past] is the real series (a 2 dp ink line over a 10 % area of
 * the same colour, ending in a dot with a ring at its last point); [projection] continues from today to the end of the
 * period as a dashed, lighter line, and a hairline marks [todayX]. One y axis with gridlines, the zero line a bit stronger
 * when the balance goes negative. Touching or dragging shows a crosshair and a tooltip with the date and the balance.
 * [contentDescription] is the summary read by screen readers.
 */
@Composable
fun BalanceLineChart(
    past: List<LineChartPoint>,
    projection: List<LineChartPoint>,
    todayX: Long,
    texts: LineChartTexts,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val ink = MaterialTheme.colorScheme.primary
    // The part of the line (and its area) below zero is drawn in the negative colour
    val negative = LocalMoneyColors.current.negative
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val zeroColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = mutedColor)
    val measurer = rememberTextMeasurer()

    // The projection starts with today's balance, which the real series already ends with
    val all = remember(past, projection) {
        (past + projection.filter { p -> past.none { it.x == p.x } }).sortedBy { it.x }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var selectedDay by remember(all) { mutableStateOf<Long?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .semantics { this.contentDescription = contentDescription }
            .onSizeChanged { size = it },
    ) {
        if (all.isEmpty() || size == IntSize.Zero) return@Box
        val density = LocalDensity.current
        val ticks = remember(all) { niceTicks(all.minOf { it.y }, all.maxOf { it.y }) }
        val yLabels = ticks.values.map { measurer.measure(texts.formatY(it), labelStyle) }
        val labelWidth = yLabels.maxOf { it.size.width }
        val plot = with(density) {
            PlotArea(
                left = labelWidth + 8.dp.toPx(),
                top = 18.dp.toPx(),
                right = size.width - 8.dp.toPx(),
                bottom = size.height - 24.dp.toPx(),
            )
        }
        val xMin = all.first().x
        val xMax = maxOf(all.last().x, xMin + 1)
        val xScale = LinearScale(xMin.toDouble(), xMax.toDouble(), plot.left, plot.right)
        val yScale = LinearScale(ticks.min.toDouble(), ticks.max.toDouble(), plot.bottom, plot.top)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .chartTouch(key = all) { touchX ->
                    selectedDay = xScale.invert(touchX).roundToLong().coerceIn(xMin, xMax)
                },
        ) {
            drawGrid(plot, ticks, yScale, yLabels, gridColor, zeroColor)
            drawXLabels(measurer, labelStyle, plot, xScale, xMin, xMax, texts.formatXAxis)

            // The split sits half a stroke below zero, so a balance of exactly zero stays ink (zero is not negative)
            val zeroY = yScale.map(0.0).coerceIn(plot.top, plot.bottom) + 1.dp.toPx()
            val hasNegative = all.any { it.y < 0 }
            // Runs [block] twice: clipped to above zero with the ink and to below zero with the negative colour
            val canvasSize = this.size
            fun splitByZero(block: DrawScope.(Color) -> Unit) {
                if (!hasNegative) {
                    block(ink)
                    return
                }
                clipRect(left = 0f, top = 0f, right = canvasSize.width, bottom = zeroY) { block(ink) }
                clipRect(left = 0f, top = zeroY, right = canvasSize.width, bottom = canvasSize.height) { block(negative) }
            }
            val pastPath = stepPath(past, xScale, yScale)
            if (past.size > 1) {
                val area = Path().apply {
                    addPath(pastPath)
                    lineTo(xScale.map(past.last().x.toDouble()), plot.bottom)
                    lineTo(xScale.map(past.first().x.toDouble()), plot.bottom)
                    close()
                }
                splitByZero { color -> drawPath(area, color.copy(alpha = 0.10f)) }
            }
            val stroke = 2.dp.toPx()
            if (projection.size > 1) {
                val projectionPath = stepPath(projection, xScale, yScale)
                val dashed = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                )
                splitByZero { color -> drawPath(projectionPath, color.copy(alpha = 0.45f), style = dashed) }
            }
            val solid = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            splitByZero { color -> drawPath(pastPath, color, style = solid) }

            // Today: a hairline with its label, when there is a projection to the right of it
            if (todayX < xMax) {
                val x = xScale.map(todayX.toDouble())
                drawLine(mutedColor.copy(alpha = 0.5f), Offset(x, plot.top), Offset(x, plot.bottom), strokeWidth = 1.dp.toPx())
                val label = measurer.measure(texts.todayLabel, labelStyle)
                drawText(label, topLeft = Offset(x + 4.dp.toPx(), 0f))
            }
            past.lastOrNull()?.let { last ->
                drawEndDot(Offset(xScale.map(last.x.toDouble()), yScale.map(last.y.toDouble())), if (last.y < 0) negative else ink, backgroundColor)
            }
            selectedDay?.let { day ->
                val p = LineChartPoint(day, stepValueAt(all.map { it.x }, all.map { it.y }, day) ?: return@let)
                val x = xScale.map(p.x.toDouble())
                drawLine(ink.copy(alpha = 0.5f), Offset(x, plot.top), Offset(x, plot.bottom), strokeWidth = 1.dp.toPx())
                drawEndDot(Offset(x, yScale.map(p.y.toDouble())), if (p.y < 0) negative else ink, backgroundColor)
            }
        }

        selectedDay?.let { day ->
            val p = LineChartPoint(day, stepValueAt(all.map { it.x }, all.map { it.y }, day) ?: return@let)
            val px = xScale.map(p.x.toDouble())
            val py = yScale.map(p.y.toDouble())
            val gap = with(density) { 10.dp.toPx() }
            val x = (px - tooltipSize.width / 2f).coerceIn(0f, (size.width - tooltipSize.width).toFloat().coerceAtLeast(0f))
            val above = py - tooltipSize.height - gap
            // Below the point when there is no room above it
            val y = if (above >= 0f) above else py + gap
            ChartTooltip(
                title = texts.formatXTooltip(p.x),
                rows = listOf(
                    TooltipRow(
                        label = if (p.x > todayX) "${texts.seriesLabel} (${texts.projectedLabel})" else texts.seriesLabel,
                        value = texts.formatValue(p.y),
                    ),
                ),
                modifier = Modifier
                    .onSizeChanged { tooltipSize = it }
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) },
            )
        }
    }
}

private val CHART_HEIGHT = 220.dp

/** Horizontal gridlines with their labels at the left; the zero line is stronger when the axis goes below zero. */
internal fun DrawScope.drawGrid(
    plot: PlotArea,
    ticks: AxisTicks,
    yScale: LinearScale,
    labels: List<TextLayoutResult>,
    gridColor: Color,
    zeroColor: Color,
) {
    ticks.values.forEachIndexed { i, value ->
        val y = yScale.map(value.toDouble())
        val isZero = value == 0L && ticks.min < 0
        drawLine(
            color = if (isZero) zeroColor else gridColor,
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = if (isZero) 1.5.dp.toPx() else 1.dp.toPx(),
        )
        val label = labels[i]
        drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
    }
}

/** About four date labels under the plot, the outer ones kept inside the canvas. */
private fun DrawScope.drawXLabels(
    measurer: TextMeasurer,
    style: TextStyle,
    plot: PlotArea,
    xScale: LinearScale,
    xMin: Long,
    xMax: Long,
    format: (Long) -> String,
) {
    evenPositions(xMin, xMax, 4).distinct().forEach { x ->
        val layout = measurer.measure(format(x), style)
        val center = xScale.map(x.toDouble())
        val left = (center - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(layout, topLeft = Offset(left, plot.bottom + 6.dp.toPx()))
    }
}

/** A dot of 10 dp with a 2 dp ring in the colour of the surface it sits on. */
internal fun DrawScope.drawEndDot(center: Offset, color: Color, ring: Color) {
    drawCircle(ring, radius = 7.dp.toPx(), center = center)
    drawCircle(color, radius = 5.dp.toPx(), center = center)
}

/**
 * A balance changes on a date and holds until the next change, so the line goes horizontally to the next point's day and
 * then vertically to its value (step after), instead of a diagonal that never happened.
 */
private fun stepPath(points: List<LineChartPoint>, xScale: LinearScale, yScale: LinearScale): Path {
    val path = Path()
    points.forEachIndexed { i, p ->
        val x = xScale.map(p.x.toDouble())
        val y = yScale.map(p.y.toDouble())
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, yScale.map(points[i - 1].y.toDouble()))
            path.lineTo(x, y)
        }
    }
    return path
}
