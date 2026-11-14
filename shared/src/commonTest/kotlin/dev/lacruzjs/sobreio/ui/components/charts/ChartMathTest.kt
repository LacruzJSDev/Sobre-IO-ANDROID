package dev.lacruzjs.sobreio.ui.components.charts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChartMathTest {

    @Test
    fun ticksUseRoundStepsAndCoverTheData() {
        val ticks = niceTicks(120_000, 800_000)
        assertTrue(ticks.min <= 120_000)
        assertTrue(ticks.max >= 800_000)
        assertTrue(ticks.values.zipWithNext().all { (a, b) -> b - a == ticks.step })
        assertEquals(0L, ticks.step % 100)
    }

    @Test
    fun ticksStartAtZeroForAColumnAxis() {
        assertEquals(listOf(0L, 5_000L, 10_000L), niceTicks(0, 10_000, 3).values)
    }

    @Test
    fun ticksIncludeNegativeValuesOnARoundGrid() {
        val ticks = niceTicks(-25_000, 130_000)
        assertTrue(ticks.min <= -25_000 && ticks.min % ticks.step == 0L)
        assertTrue(ticks.max >= 130_000)
        assertTrue(0L in ticks.values)
    }

    @Test
    fun aFlatRangeStillHasTwoTicks() {
        val ticks = niceTicks(5_000, 5_000)
        assertEquals(2, ticks.values.size)
        assertTrue(ticks.min <= 5_000 && ticks.max >= 5_000)
        assertEquals(2, niceTicks(0, 0).values.size)
    }

    @Test
    fun compactAmountsShortenThousandsAndMillions() {
        assertEquals("850", compactAmount(85_000, "en"))
        assertEquals("1K", compactAmount(100_000, "en"))
        assertEquals("1.5K", compactAmount(150_000, "en"))
        assertEquals("1,5K", compactAmount(150_000, "es-ES"))
        assertEquals("-2K", compactAmount(-200_000, "en"))
        assertEquals("2M", compactAmount(200_000_000, "en"))
        assertEquals("0", compactAmount(0, "en"))
        assertEquals("0", compactAmount(-20, "en"))
    }

    @Test
    fun scaleMapsAndInverts() {
        val scale = LinearScale(0.0, 10.0, 100f, 200f)
        assertEquals(100f, scale.map(0.0))
        assertEquals(150f, scale.map(5.0))
        assertEquals(200f, scale.map(10.0))
        assertEquals(5.0, scale.invert(150f))
        // y axes run upwards on the data and downwards on screen
        assertEquals(300f, LinearScale(0.0, 100.0, 300f, 100f).map(0.0))
        assertEquals(150f, LinearScale(3.0, 3.0, 100f, 200f).map(3.0))
    }

    @Test
    fun nearestIndexPicksTheClosestPoint() {
        val xs = listOf(0.0, 10.0, 20.0)
        assertEquals(0, nearestIndex(xs, -5.0))
        assertEquals(1, nearestIndex(xs, 6.0))
        assertEquals(2, nearestIndex(xs, 100.0))
        assertNull(nearestIndex(emptyList(), 1.0))
    }

    @Test
    fun evenPositionsIncludeBothEnds() {
        assertEquals(listOf(0L, 10L, 20L, 30L), evenPositions(0, 30, 4))
        assertEquals(listOf(5L), evenPositions(5, 5, 4))
    }

    @Test
    fun slotsAreClampedToTheChart() {
        assertEquals(0, slotAt(-10f, 300f, 3))
        assertEquals(1, slotAt(150f, 300f, 3))
        assertEquals(2, slotAt(999f, 300f, 3))
    }

    @Test
    fun columnsAreNeverWiderThanTheLimitAndStayInTheSlot() {
        val wide = columnLayout(slot = 200f, seriesCount = 2, maxBar = 24f, gap = 2f)
        assertEquals(24f, wide.barWidth)
        assertEquals((200f - (48f + 2f)) / 2f, wide.groupStart)
        val narrow = columnLayout(slot = 40f, seriesCount = 2, maxBar = 24f, gap = 2f)
        assertTrue(narrow.barWidth < 24f)
        assertTrue(narrow.groupStart >= 0f)
    }

    @Test
    fun aStepSeriesHoldsItsValueUntilTheNextPoint() {
        val xs = listOf(10L, 20L, 30L)
        val ys = listOf(0L, 800L, 500L)
        assertEquals(0L, stepValueAt(xs, ys, 5))
        assertEquals(0L, stepValueAt(xs, ys, 19))
        assertEquals(800L, stepValueAt(xs, ys, 20))
        assertEquals(800L, stepValueAt(xs, ys, 29))
        assertEquals(500L, stepValueAt(xs, ys, 99))
        assertNull(stepValueAt(emptyList(), emptyList(), 1))
    }
}
