package dev.lacruzjs.sobreio.ui.components

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SmallHelpersTest {

    @Test
    fun aGroupInitialIsItsFirstLetterInUpperCase() {
        assertEquals("C", groupInitial("  casa"))
        assertEquals("Á", groupInitial("árbol"))
        assertEquals("", groupInitial("   "))
    }

    @Test
    fun theDatePickerDayIsTheSameDayInEveryTimeZone() {
        // The picker counts UTC midnights: a day goes in and comes back unchanged, whatever the device's zone
        listOf(LocalDate(2026, 1, 1), LocalDate(2026, 3, 29), LocalDate(2026, 10, 25), LocalDate(2028, 2, 29)).forEach { day ->
            assertEquals(day, day.toPickerMillis().toPickerDate())
        }
        assertEquals(0L, LocalDate(1970, 1, 1).toPickerMillis())
    }
}
