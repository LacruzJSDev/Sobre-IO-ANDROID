package dev.lacruzjs.sobreio.ui.components.charts

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * Pure maths of the charts (scales, clean axis ticks, compact labels, nearest point), kept apart from drawing so it is
 * tested without a screen. Amounts are cents; x values of the line chart are epoch days.
 */

/** Evenly spaced axis values: [values] runs from the first tick (<= data minimum) to the last (>= data maximum). */
data class AxisTicks(val values: List<Long>, val step: Long) {
    val min: Long get() = values.first()
    val max: Long get() = values.last()
}

/**
 * Round tick values for an axis covering [minValue]..[maxValue] (cents) with about [targetCount] ticks: the step is
 * 1, 2 or 5 times a power of ten, never below one whole currency unit (100 cents), and the ends are multiples of it.
 * A flat range still gets two ticks.
 */
fun niceTicks(minValue: Long, maxValue: Long, targetCount: Int = 5): AxisTicks {
    val low = minOf(minValue, maxValue)
    val high = maxOf(minValue, maxValue)
    val span = (high - low).coerceAtLeast(1)
    val rawStep = span.toDouble() / (targetCount - 1).coerceAtLeast(1)
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val fraction = rawStep / magnitude
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = (niceFraction * magnitude).roundToLong().coerceAtLeast(MIN_TICK_STEP_CENTS)
    val first = low.floorDiv(step) * step
    var last = high.floorDiv(step) * step
    if (last < high) last += step
    if (last == first) last = first + step
    val values = generateSequence(first) { it + step }.takeWhile { it <= last }.toList()
    return AxisTicks(values, step)
}

private const val MIN_TICK_STEP_CENTS = 100L

/**
 * A short axis label for [cents]: whole units under a thousand (`850`), `1K`, `1.5K` and `2M` above, with the decimal
 * separator of [languageTag]. No currency: the axis title or the tooltip says it.
 */
fun compactAmount(cents: Long, languageTag: String): String {
    val units = abs(cents) / 100.0
    val sign = if (cents < 0) "-" else ""
    if (units < 1_000) {
        val whole = units.roundToLong()
        return if (whole == 0L) "0" else sign + whole
    }
    val scaled = if (units >= 1_000_000) units / 1_000_000 else units / 1_000
    val suffix = if (units >= 1_000_000) "M" else "K"
    val tenths = (scaled * 10).roundToLong()
    val separator = if (languageTag.substringBefore('-').substringBefore('_').lowercase() == "es") "," else "."
    val number = if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}$separator${tenths % 10}"
    return "$sign$number$suffix"
}

/** Maps a value of [domainMin]..[domainMax] to [rangeStart]..[rangeEnd] (pixels) and back. A flat domain maps to the middle. */
class LinearScale(
    private val domainMin: Double,
    private val domainMax: Double,
    private val rangeStart: Float,
    private val rangeEnd: Float,
) {
    fun map(value: Double): Float {
        val span = domainMax - domainMin
        if (span == 0.0) return (rangeStart + rangeEnd) / 2f
        return (rangeStart + ((value - domainMin) / span) * (rangeEnd - rangeStart)).toFloat()
    }

    fun invert(pixel: Float): Double {
        val span = rangeEnd - rangeStart
        if (span == 0f) return domainMin
        return domainMin + ((pixel - rangeStart) / span) * (domainMax - domainMin)
    }
}

/** Index of the entry of [xs] (ascending) closest to [x], or null when [xs] is empty. */
fun nearestIndex(xs: List<Double>, x: Double): Int? {
    if (xs.isEmpty()) return null
    var best = 0
    var bestDistance = abs(xs[0] - x)
    for (i in 1 until xs.size) {
        val distance = abs(xs[i] - x)
        if (distance < bestDistance) {
            best = i
            bestDistance = distance
        }
    }
    return best
}

/** [count] positions evenly spread from [min] to [max], both included (one position when they coincide). */
fun evenPositions(min: Long, max: Long, count: Int): List<Long> {
    if (count <= 1 || max <= min) return listOf(min)
    return (0 until count).map { i -> min + ((max - min) * i) / (count - 1) }
}

/** The slot (0 until [count]) of a plot [width] wide that contains pixel [x], clamped to the ends. */
fun slotAt(x: Float, width: Float, count: Int): Int {
    if (count <= 0 || width <= 0f) return 0
    return (x / width * count).toInt().coerceIn(0, count - 1)
}

/**
 * The value of a step series at [x]: the value of the last point at or before it, or the first value when [x] is before
 * the series. [xs] is ascending and parallel to [ys]; null when empty.
 */
fun stepValueAt(xs: List<Long>, ys: List<Long>, x: Long): Long? {
    if (xs.isEmpty()) return null
    val index = xs.indexOfLast { it <= x }
    return ys[if (index < 0) 0 else index]
}
