package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MoneyColorsTest {

    private val variants = listOf(
        Triple("light", false, false),
        Triple("dark", true, false),
        Triple("light colour-blind", false, true),
        Triple("dark colour-blind", true, true),
    )

    private fun ink(dark: Boolean) = if (dark) DarkInk else LightInk

    @Test
    fun flowIsInflowWhenPositiveAndOutflowOtherwiseInEveryVariant() {
        for ((name, dark, blind) in variants) {
            val c = moneyColors(dark, blind)
            assertEquals(c.inflow, moneyColorFor(MoneyRole.Flow, 1, c, ink(dark)), name)
            assertEquals(c.outflow, moneyColorFor(MoneyRole.Flow, -1, c, ink(dark)), name)
            assertEquals(c.outflow, moneyColorFor(MoneyRole.Flow, 0, c, ink(dark)), name)
        }
    }

    @Test
    fun transferIsNeutralForAnySign() {
        for ((name, dark, blind) in variants) {
            val c = moneyColors(dark, blind)
            for (amount in listOf(-500L, 0L, 500L)) {
                assertEquals(c.neutral, moneyColorFor(MoneyRole.Transfer, amount, c, ink(dark)), name)
            }
        }
    }

    @Test
    fun balanceIsInkUnlessNegative() {
        for ((name, dark, blind) in variants) {
            val c = moneyColors(dark, blind)
            assertEquals(ink(dark), moneyColorFor(MoneyRole.Balance, 0, c, ink(dark)), name)
            assertEquals(ink(dark), moneyColorFor(MoneyRole.Balance, 100, c, ink(dark)), name)
            assertEquals(c.negative, moneyColorFor(MoneyRole.Balance, -1, c, ink(dark)), name)
        }
    }

    @Test
    fun plainIsAlwaysInk() {
        for ((name, dark, blind) in variants) {
            val c = moneyColors(dark, blind)
            for (amount in listOf(-5L, 0L, 5L)) {
                assertEquals(ink(dark), moneyColorFor(MoneyRole.Plain, amount, c, ink(dark)), name)
            }
        }
    }

    @Test
    fun paletteMatchesTheDesign() {
        assertEquals(MoneyColors(Color(0xFF1E874B), LightInk, LightMuted, LightRed), moneyColors(false, false))
        assertEquals(MoneyColors(Color(0xFF4CC27E), DarkInk, DarkMuted, DarkRed), moneyColors(true, false))
        assertEquals(MoneyColors(Color(0xFF2369C4), LightInk, LightMuted, Color(0xFFC2410C)), moneyColors(false, true))
        assertEquals(MoneyColors(Color(0xFF6AA6F0), DarkInk, DarkMuted, Color(0xFFF38744)), moneyColors(true, true))
    }

    @Test
    fun colourBlindModeChangesOnlyInflowAndNegative() {
        for (dark in listOf(false, true)) {
            val normal = moneyColors(dark, false)
            val blind = moneyColors(dark, true)
            assertNotEquals(normal.inflow, blind.inflow)
            assertNotEquals(normal.negative, blind.negative)
            assertEquals(normal.outflow, blind.outflow)
            assertEquals(normal.neutral, blind.neutral)
        }
    }
}
