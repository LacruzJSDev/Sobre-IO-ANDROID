package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountDetailTest {

    private val projection = listOf(
        ProjectionPoint(LocalDate(2026, 11, 14), 100, 0),
        ProjectionPoint(LocalDate(2026, 11, 18), -40, -140),
        ProjectionPoint(LocalDate(2026, 11, 22), -40, 0),
        ProjectionPoint(LocalDate(2026, 11, 28), 60, 100),
    )

    @Test
    fun theShortfallIsOnTheFirstDayAtTheMinimum() {
        assertEquals(LocalDate(2026, 11, 18), shortfallDate(projection, -40))
    }

    @Test
    fun withoutAnExactMatchTheLowestRowIsUsed() {
        assertEquals(LocalDate(2026, 11, 18), shortfallDate(projection, -75))
    }

    @Test
    fun noRowsNoDate() {
        assertNull(shortfallDate(emptyList(), 0))
    }
}
