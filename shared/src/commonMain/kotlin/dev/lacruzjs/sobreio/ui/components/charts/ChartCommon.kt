package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** The rectangle of a chart (in pixels) where the data is drawn, inside the axis labels. */
internal class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** One line of a chart tooltip: an optional colour swatch, a [label] and its [value]. */
data class TooltipRow(val label: String, val value: String, val color: Color? = null)

/**
 * The small box shown over a chart while a point or a group is selected: [title] (a date) and one row per value. Ink
 * background with paper text, like the snackbar, so it stands out on the soft tile the charts sit on. Flat, no shadow.
 */
@Composable
fun ChartTooltip(title: String, rows: List<TooltipRow>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
        rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row.color != null) {
                    Box(Modifier.size(8.dp).background(row.color, RoundedCornerShape(2.dp)))
                }
                Text(text = row.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text(text = row.value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

/** An entry of a [ChartLegend]. */
data class LegendItem(val label: String, val color: Color)

/** Legend of a chart with several series: a colour swatch and the name of each. Single-series charts need none. */
@Composable
fun ChartLegend(items: List<LegendItem>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).background(item.color, RoundedCornerShape(2.dp)))
                Text(text = item.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Reports the x position of a touch while the finger is down and moves horizontally: on press and on every move. A
 * gesture that goes mostly vertical is left alone (the page scrolls) and clears the selection through [onScrollAway]. Whole-chart hit area, far bigger than the marks.
 */
@Composable
internal fun Modifier.chartTouch(key: Any?, onScrollAway: () -> Unit = {}, onTouch: (Float) -> Unit): Modifier {
    val slop = LocalViewConfiguration.current.touchSlop
    return pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onTouch(down.position.x)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) break
                val dx = abs(change.position.x - down.position.x)
                val dy = abs(change.position.y - down.position.y)
                if (dy > slop && dy > dx) {
                    // The page is scrolling: the selection goes away
                    onScrollAway()
                    break
                }
                onTouch(change.position.x)
                if (dx > slop) change.consume()
            }
        }
    }
}
