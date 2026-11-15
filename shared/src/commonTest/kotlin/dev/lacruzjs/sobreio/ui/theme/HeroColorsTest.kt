package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HeroColorsTest {

    private val variants = listOf(
        Triple("light", false, false),
        Triple("dark", true, false),
        Triple("light colour-blind", false, true),
        Triple("dark colour-blind", true, true),
    )

    private fun contrast(a: Color, b: Color): Float {
        val hi = maxOf(a.luminance(), b.luminance())
        val lo = minOf(a.luminance(), b.luminance())
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun containerFollowsTheDesign() {
        assertEquals(Color(0xFF0C0C0C), heroColors(false, false).container)
        assertEquals(LightPaper, heroColors(false, false).onContainer)
        assertEquals(Color(0xFF2D2D2A), heroColors(true, false).container)
        assertEquals(Color(0xFFF4F4F4), heroColors(true, false).onContainer)
    }

    @Test
    fun cardStaysDarkAndDistinctFromThePaperInBothModes() {
        for ((name, dark, blind) in variants) {
            val c = heroColors(dark, blind)
            assertTrue(c.container.luminance() < 0.05f, "$name: dark card")
            assertTrue(contrast(c.container, c.onContainer) >= 7f, "$name: text contrast")
        }
        assertNotEquals(DarkPaper, heroColors(true, false).container)
        assertNotEquals(DarkSoft, heroColors(true, false).container)
    }

    @Test
    fun belowZeroChipIsReadableInEveryVariant() {
        for ((name, dark, blind) in variants) {
            val c = heroColors(dark, blind)
            assertTrue(contrast(c.chipContainer, c.chipContent) >= 4.5f, "$name: chip text on its background")
            assertTrue(contrast(c.container, c.chipContainer) >= 7f, "$name: chip stands out on the card")
        }
    }

    @Test
    fun negativeColourOnTheCardIsTheDarkPalettes() {
        assertEquals(DarkRed, heroColors(false, false).negativeOnContainer)
        assertEquals(DarkNegativeColorBlind, heroColors(true, true).negativeOnContainer)
        assertEquals(LightRed, heroColors(true, false).chipContent)
        assertEquals(LightNegativeColorBlind, heroColors(false, true).chipContent)
    }

    @Test
    fun theMainFigureIsAlwaysOnContainerAndOnlyNegativeOnesGetTheChip() {
        for ((name, dark, blind) in variants) {
            val c = heroColors(dark, blind)
            for (amount in listOf(-500L, 0L, 500L)) {
                assertEquals(c.onContainer, heroFigureColor(c, amount), name)
            }
        }
        assertTrue(showsBelowZeroChip(-1))
        assertFalse(showsBelowZeroChip(0))
        assertFalse(showsBelowZeroChip(1))
    }
}
