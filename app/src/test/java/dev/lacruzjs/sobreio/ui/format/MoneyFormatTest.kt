package dev.lacruzjs.sobreio.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatTest {

    private fun euros(text: String) = "$text €"

    @Test
    fun groupsThousandsEvenWithFourDigits() {
        assertEquals(euros("1.298,15"), formatMoney(129_815, "EUR"))
        assertEquals(euros("1.234.567,89"), formatMoney(123_456_789, "EUR"))
    }

    @Test
    fun alwaysShowsTwoDecimals() {
        assertEquals(euros("0,00"), formatMoney(0, "EUR"))
        assertEquals(euros("1,00"), formatMoney(100, "EUR"))
        assertEquals(euros("0,05"), formatMoney(5, "EUR"))
    }

    @Test
    fun negativeAmountsKeepTheirSign() {
        assertEquals(euros("-0,05"), formatMoney(-5, "EUR"))
        assertEquals(euros("-1.298,15"), formatMoney(-129_815, "EUR"))
    }

    @Test
    fun extremeAmountsDoNotOverflow() {
        assertEquals(euros("-92.233.720.368.547.758,08"), formatMoney(Long.MIN_VALUE, "EUR"))
    }
}
