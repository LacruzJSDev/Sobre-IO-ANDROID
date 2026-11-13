package dev.lacruzjs.sobreio.ui.format

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DateFormatTest {

    private val instant = Instant.parse("2026-11-13T10:00:00Z")

    @Test
    fun englishIsTheDefault() {
        assertEquals("Nov 13, 2026", formatDate(instant, "en-GB", TimeZone.UTC))
        assertEquals("Nov 13, 2026", formatDate(instant, "fr-FR", TimeZone.UTC))
    }

    @Test
    fun spanishPutsTheDayFirst() {
        assertEquals("13 nov 2026", formatDate(instant, "es-ES", TimeZone.UTC))
    }

    @Test
    fun theDayIsReadInTheGivenTimeZone() {
        assertEquals("Nov 14, 2026", formatDate(Instant.parse("2026-11-13T23:30:00Z"), "en", TimeZone.of("Europe/Madrid")))
    }
}
