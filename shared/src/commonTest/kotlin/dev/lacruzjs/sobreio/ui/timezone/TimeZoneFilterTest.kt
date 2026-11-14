package dev.lacruzjs.sobreio.ui.timezone

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeZoneFilterTest {

    private val zones = listOf("America/New_York", "Europe/London", "Europe/Madrid")

    @Test
    fun theCurrentZoneComesFirstWhenNothingIsTyped() {
        assertEquals(listOf("Europe/Madrid", "America/New_York", "Europe/London"), filterTimeZones("", "Europe/Madrid", zones))
    }

    @Test
    fun filtersIgnoringCaseAndSpacesMatchUnderscores() {
        assertEquals(listOf("America/New_York"), filterTimeZones("new york", "Europe/Madrid", zones))
        assertEquals(listOf("Europe/Madrid"), filterTimeZones("MAD", "Europe/Madrid", zones))
    }

    @Test
    fun theCurrentZoneIsOnlyListedWhenItMatches() {
        assertEquals(listOf("America/New_York"), filterTimeZones("new", "Europe/Madrid", zones))
        assertEquals(emptyList(), filterTimeZones("zzz", "Europe/Madrid", zones))
    }
}
