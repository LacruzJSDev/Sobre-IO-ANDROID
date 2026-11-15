package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementDaysTest {

    private fun movement(id: String, date: LocalDate) = Movement(
        id = id, source = MovementSource.Transaction, accountId = "a", counterAccountId = null, envelopeId = "e",
        kind = MovementKind.Expense, amountCents = -100, date = date, notes = null, createdBy = null, createdAt = "",
    )

    private val today = LocalDate(2026, 11, 14)

    @Test
    fun groupsConsecutiveMovementsOfTheSameDay() {
        val list = listOf(
            movement("1", today),
            movement("2", today),
            movement("3", LocalDate(2026, 11, 13)),
            movement("4", LocalDate(2026, 11, 12)),
            movement("5", LocalDate(2026, 11, 12)),
        )

        val sections = groupByDay(list, today)

        assertEquals(listOf(today, LocalDate(2026, 11, 13), LocalDate(2026, 11, 12)), sections.map { it.date })
        assertEquals(listOf(listOf("1", "2"), listOf("3"), listOf("4", "5")), sections.map { s -> s.movements.map { it.id } })
    }

    @Test
    fun marksTodayAndYesterday() {
        val sections = groupByDay(
            listOf(movement("1", today), movement("2", LocalDate(2026, 11, 13)), movement("3", LocalDate(2026, 11, 12))),
            today,
        )

        assertEquals(listOf(DayRelative.Today, DayRelative.Yesterday, DayRelative.Other), sections.map { it.relative })
    }

    @Test
    fun yesterdayAcrossAMonthBoundary() {
        val sections = groupByDay(listOf(movement("1", LocalDate(2026, 10, 31))), LocalDate(2026, 11, 1))

        assertEquals(DayRelative.Yesterday, sections.single().relative)
    }

    @Test
    fun withoutATodayEveryDayIsPlain() {
        val sections = groupByDay(listOf(movement("1", today)), null)

        assertEquals(DayRelative.Other, sections.single().relative)
    }

    @Test
    fun noMovementsNoSections() {
        assertTrue(groupByDay(emptyList(), today).isEmpty())
    }

    private val context = DateContext(
        today = today,
        currentPeriod = PeriodRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)),
        previousPeriod = PeriodRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)),
    )

    @Test
    fun presetsGiveTheirRanges() {
        assertEquals(LocalDate(2026, 11, 1) to LocalDate(2026, 11, 30), datePresetRange(DatePreset.ThisPeriod, context))
        assertEquals(LocalDate(2026, 10, 1) to LocalDate(2026, 10, 31), datePresetRange(DatePreset.PreviousPeriod, context))
        assertEquals(LocalDate(2026, 8, 15) to today, datePresetRange(DatePreset.Last3Months, context))
        assertEquals(LocalDate(2026, 1, 1) to today, datePresetRange(DatePreset.ThisYear, context))
        assertNull(datePresetRange(DatePreset.Custom, context))
    }

    @Test
    fun withoutAPreviousPeriodThereIsNoRange() {
        assertNull(datePresetRange(DatePreset.PreviousPeriod, context.copy(previousPeriod = null)))
    }
}
