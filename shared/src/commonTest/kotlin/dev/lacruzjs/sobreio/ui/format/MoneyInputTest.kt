package dev.lacruzjs.sobreio.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyInputTest {

    @Test
    fun blankIsZero() {
        assertEquals(0L, parseMoneyInput(""))
        assertEquals(0L, parseMoneyInput("   "))
    }

    @Test
    fun acceptsCommaOrDotAsTheOnlyDecimalSeparator() {
        assertEquals(1250L, parseMoneyInput("12,5"))
        assertEquals(1250L, parseMoneyInput("12.5"))
        assertEquals(1215L, parseMoneyInput("12,15"))
        assertEquals(1200L, parseMoneyInput("12"))
        assertEquals(1200L, parseMoneyInput("12,"))
        assertEquals(50L, parseMoneyInput(",5"))
        assertEquals(5L, parseMoneyInput("0,05"))
    }

    @Test
    fun acceptsALeadingMinus() {
        assertEquals(-25050L, parseMoneyInput("-250,5"))
        assertEquals(-100L, parseMoneyInput(" -1 "))
        assertEquals(0L, parseMoneyInput("-0"))
    }

    @Test
    fun rejectsThousandsSeparatorsAndMoreThanTwoDecimals() {
        assertNull(parseMoneyInput("1.298,15"))
        assertNull(parseMoneyInput("1,298.15"))
        assertNull(parseMoneyInput("1.298"))
        assertNull(parseMoneyInput("12,345"))
    }

    @Test
    fun rejectsTextAndStraySigns() {
        listOf("-", ",", ".", "abc", "12a", "--1", "1-", "+5", "1 000", "-,").forEach {
            assertNull(parseMoneyInput(it), it)
        }
    }

    @Test
    fun rejectsAmountsTooBigForCents() {
        assertNull(parseMoneyInput("1234567890123"))
    }
}
