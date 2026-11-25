package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** CIE Lab (D65) of an sRGB colour. */
private fun lab(color: Color): DoubleArray {
    fun lin(c: Float): Double = c.toDouble().let { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
    val r = lin(color.red)
    val g = lin(color.green)
    val b = lin(color.blue)
    val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
    fun f(t: Double) = if (t > 0.008856) cbrt(t) else 7.787 * t + 16.0 / 116.0
    return doubleArrayOf(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
}

private fun deg(rad: Double) = rad * 180 / PI
private fun rad(deg: Double) = deg * PI / 180

/** Perceived difference between two colours (CIEDE2000): about 2 is barely visible, 10 and more is clearly different. */
fun deltaE2000(first: Color, second: Color): Double {
    val (l1, a1, b1) = lab(first).let { Triple(it[0], it[1], it[2]) }
    val (l2, a2, b2) = lab(second).let { Triple(it[0], it[1], it[2]) }
    val cBar = (hypot(a1, b1) + hypot(a2, b2)) / 2
    val g = 0.5 * (1 - sqrt(cBar.pow(7) / (cBar.pow(7) + 25.0.pow(7))))
    val a1p = (1 + g) * a1
    val a2p = (1 + g) * a2
    val c1p = hypot(a1p, b1)
    val c2p = hypot(a2p, b2)
    val h1 = (deg(atan2(b1, a1p)) + 360) % 360
    val h2 = (deg(atan2(b2, a2p)) + 360) % 360
    val dL = l2 - l1
    val dC = c2p - c1p
    val dh = when {
        c1p * c2p == 0.0 -> 0.0
        abs(h2 - h1) <= 180 -> h2 - h1
        h2 > h1 -> h2 - h1 - 360
        else -> h2 - h1 + 360
    }
    val dH = 2 * sqrt(c1p * c2p) * sin(rad(dh / 2))
    val lBar = (l1 + l2) / 2
    val cBarP = (c1p + c2p) / 2
    val hBar = when {
        c1p * c2p == 0.0 -> h1 + h2
        abs(h1 - h2) <= 180 -> (h1 + h2) / 2
        h1 + h2 < 360 -> (h1 + h2 + 360) / 2
        else -> (h1 + h2 - 360) / 2
    }
    val t = 1 - 0.17 * cos(rad(hBar - 30)) + 0.24 * cos(rad(2 * hBar)) + 0.32 * cos(rad(3 * hBar + 6)) - 0.20 * cos(rad(4 * hBar - 63))
    val sl = 1 + 0.015 * (lBar - 50).pow(2) / sqrt(20 + (lBar - 50).pow(2))
    val sc = 1 + 0.045 * cBarP
    val sh = 1 + 0.015 * cBarP * t
    val rt = -2 * sqrt(cBarP.pow(7) / (cBarP.pow(7) + 25.0.pow(7))) * sin(rad(60 * exp(-((hBar - 275) / 25).pow(2))))
    return sqrt((dL / sl).pow(2) + (dC / sc).pow(2) + (dH / sh).pow(2) + rt * (dC / sc) * (dH / sh))
}

/** The closest pair of [colors], as a readable message and its distance. */
fun closestPair(colors: List<Pair<String, Color>>): Pair<String, Double> =
    colors.indices.flatMap { i -> (i + 1 until colors.size).map { j -> i to j } }
        .map { (i, j) -> "${colors[i].first}/${colors[j].first}" to deltaE2000(colors[i].second, colors[j].second) }
        .minBy { it.second }
