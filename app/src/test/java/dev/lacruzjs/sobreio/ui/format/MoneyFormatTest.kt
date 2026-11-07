package dev.lacruzjs.sobreio.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {

    private val spanish = Locale.forLanguageTag("es-ES")
    private val english = Locale.forLanguageTag("en-GB")

    private fun euros(text: String) = "$text €"

    @Test
    fun spanishGroupsThousandsEvenWithFourDigits() {
        assertEquals(euros("1.298,15"), formatMoney(129_815, "EUR", spanish))
        assertEquals(euros("1.234.567,89"), formatMoney(123_456_789, "EUR", spanish))
    }

    @Test
    fun spanishAlwaysShowsTwoDecimals() {
        assertEquals(euros("0,00"), formatMoney(0, "EUR", spanish))
        assertEquals(euros("1,00"), formatMoney(100, "EUR", spanish))
        assertEquals(euros("0,05"), formatMoney(5, "EUR", spanish))
    }

    @Test
    fun negativeAmountsKeepTheirSign() {
        assertEquals(euros("-0,05"), formatMoney(-5, "EUR", spanish))
        assertEquals(euros("-1.298,15"), formatMoney(-129_815, "EUR", spanish))
        assertEquals("-€1,298.15", formatMoney(-129_815, "EUR", english))
    }

    @Test
    fun englishPutsTheSymbolFirstAndSwapsSeparators() {
        assertEquals("€1,298.15", formatMoney(129_815, "EUR", english))
        assertEquals("€0.05", formatMoney(5, "EUR", english))
        assertEquals("€1,234,567.89", formatMoney(123_456_789, "EUR", english))
    }

    @Test
    fun otherLanguagesFallBackToEnglishStyle() {
        assertEquals("€1,298.15", formatMoney(129_815, "EUR", Locale.forLanguageTag("fr-FR")))
    }

    @Test
    fun extremeAmountsDoNotOverflow() {
        assertEquals(euros("-92.233.720.368.547.758,08"), formatMoney(Long.MIN_VALUE, "EUR", spanish))
    }
}
