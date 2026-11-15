package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compact projection line of the Group tab's hero card, drawn with Canvas on a dark surface: no axes, no labels. [points]
 * is the free money day by day (`group_free_projection`, x an epoch day, y cents) and the line steps on the days it
 * changes, like [BalanceLineChart]: solid up to [todayX], dashed after it, and the part below zero in [negativeColor].
 * A dot with a ring in [ringColor] marks the last day. [lineColor] is the paper colour of the hero.
 */
@Composable
fun FreeProjectionChart(
    points: List<LineChartPoint>,
    todayX: Long,
    lineColor: Color,
    negativeColor: Color,
    ringColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription },
    ) {
        if (points.isEmpty()) return@Canvas
        val pad = 8.dp.toPx()
        val xMin = points.first().x
        val xMax = maxOf(points.last().x, xMin + 1)
        val yMin = points.minOf { it.y }
        val yMax = maxOf(points.maxOf { it.y }, yMin + 1)
        val xScale = LinearScale(xMin.toDouble(), xMax.toDouble(), pad, size.width - pad)
        val yScale = LinearScale(yMin.toDouble(), yMax.toDouble(), size.height - pad, pad)
        val hasNegative = yMin < 0
        // The split sits half a stroke below zero, so a balance of exactly zero stays in the line colour
        val zeroY = yScale.map(0.0).coerceIn(0f, size.height) + 1.dp.toPx()
        val stroke = 2.dp.toPx()

        // Runs [block] twice: clipped to above zero with the line colour and to below zero with the negative colour
        fun splitByZero(block: DrawScope.(Color) -> Unit) {
            if (!hasNegative) {
                block(lineColor)
                return
            }
            clipRect(left = 0f, top = 0f, right = size.width, bottom = zeroY) { block(lineColor) }
            clipRect(left = 0f, top = zeroY, right = size.width, bottom = size.height) { block(negativeColor) }
        }

        if (hasNegative && yMax > 0) {
            val y = yScale.map(0.0)
            drawLine(lineColor.copy(alpha = 0.25f), Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1.dp.toPx())
        }
        val past = points.filter { it.x <= todayX }
        val dashed = Stroke(
            width = stroke,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
        )
        if (points.size > 1) {
            val projection = stepPath(points.filter { it.x >= todayX }, xScale, yScale)
            splitByZero { color -> drawPath(projection, color.copy(alpha = 0.7f), style = dashed) }
        }
        if (past.size > 1) {
            val solid = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = stepPath(past, xScale, yScale)
            splitByZero { color -> drawPath(path, color, style = solid) }
        }
        points.last().let { last ->
            drawEndDot(Offset(xScale.map(last.x.toDouble()), yScale.map(last.y.toDouble())), if (last.y < 0) negativeColor else lineColor, ringColor)
        }
    }
}
