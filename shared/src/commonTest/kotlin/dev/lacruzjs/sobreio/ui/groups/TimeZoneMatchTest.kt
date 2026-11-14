package dev.lacruzjs.sobreio.ui.groups

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeZoneMatchTest {

    private val zones = listOf("Europe/London", "Europe/Madrid", "America/New_York") + (1..80).map { "Etc/Z$it" }

    @Test
    fun filtersWhileTypingIgnoringCaseAndSpaces() {
        assertEquals(listOf("America/New_York"), matchingTimeZones("new york", zones))
        assertEquals(listOf("Europe/London", "Europe/Madrid"), matchingTimeZones("EUROPE", zones))
    }

    @Test
    fun neverReturnsMoreThanFifty() {
        assertEquals(50, matchingTimeZones("", zones).size)
        assertTrue(matchingTimeZones("zzz", zones).isEmpty())
    }
}
