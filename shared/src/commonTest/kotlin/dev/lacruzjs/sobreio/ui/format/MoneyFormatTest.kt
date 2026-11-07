package dev.lacruzjs.sobreio.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {

    private val spanish = "es-ES"
    private val english = "en-GB"

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
        assertEquals("€1,298.15", formatMoney(129_815, "EUR", "fr-FR"))
    }

    @Test
    fun spanishIsRecognizedByLanguageWhateverTheRegion() {
        assertEquals(euros("1.298,15"), formatMoney(129_815, "EUR", "es"))
        assertEquals(euros("1.298,15"), formatMoney(129_815, "EUR", "es_MX"))
    }

    @Test
    fun currenciesWithoutSymbolShowTheirCode() {
        assertEquals("USD\u00A01,298.15", formatMoney(129_815, "USD", english))
        assertEquals("-USD\u00A00.05", formatMoney(-5, "USD", english))
        assertEquals("1.298,15\u00A0USD", formatMoney(129_815, "USD", spanish))
    }

    @Test
    fun extremeAmountsDoNotOverflow() {
        assertEquals(euros("-92.233.720.368.547.758,08"), formatMoney(Long.MIN_VALUE, "EUR", spanish))
    }
}
