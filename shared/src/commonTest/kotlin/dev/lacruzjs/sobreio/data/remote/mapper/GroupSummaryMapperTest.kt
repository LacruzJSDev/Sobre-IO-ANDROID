package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.DueOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeSpendingDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.FreePointDto
import dev.lacruzjs.sobreio.data.remote.dto.GroupOutlookDto
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupSummaryMapperTest {

    private fun due(kind: String) =
        DueOccurrenceDto("c1", "v1", "Rent", kind, "2026-11-10", 50_000, "a1", "a2", "e1", true).toDomain()

    @Test
    fun outlookKeepsEveryFigureAndPeriodDates() {
        val outlook = GroupOutlookDto("2026-11-01", "2026-11-30", 10, 20, -10, 30, 40, 50, 60).toDomain()

        assertEquals(LocalDate(2026, 11, 1), outlook.periodStart)
        assertEquals(LocalDate(2026, 11, 30), outlook.periodEnd)
        assertEquals(-10, outlook.freeTodayCents)
        assertEquals(30, outlook.committedShortfallCents)
        assertEquals(60, outlook.freeProjectedCents)
    }

    @Test
    fun incomeEntersTheAccountAndTheRestLeaveIt() {
        assertEquals(50_000, due("income").signedCents)
        assertEquals(-50_000, due("expense").signedCents)
        assertEquals(-50_000, due("transfer").signedCents)
        assertEquals(-50_000, due("group_transfer_out").signedCents)
        assertEquals(50_000, due("group_transfer_in").signedCents)
    }

    @Test
    fun theReceivingHalfOfAContributionIsNotReviewable() {
        assertFalse(due("group_transfer_in").isReviewable)
        assertTrue(due("group_transfer_out").isReviewable)
        assertEquals(CommitmentKind.Transfer, due("transfer").kind)
    }

    @Test
    fun onlyAccountsThatRunShortBecomeShortfalls() {
        val rows = listOf(
            ForecastDto("a1", projectedMin = -5_000, shortfall = 5_000),
            ForecastDto("a2", projectedMin = 100, shortfall = 0),
        )

        val shortfalls = rows.toShortfalls()

        assertEquals(1, shortfalls.size)
        assertEquals("a1", shortfalls[0].accountId)
        assertEquals(-5_000, shortfalls[0].lowestCents)
    }

    @Test
    fun freeProjectionRowsKeepTheDayAndTheFigure() {
        val point = FreePointDto("2026-11-20", -1_500).toDomain()

        assertEquals(LocalDate(2026, 11, 20), point.day)
        assertEquals(-1_500, point.freeCents)
    }

    @Test
    fun spendingTurnsTheNegativeActivityIntoAPositiveAmount() {
        val spending = EnvelopeSpendingDto("e1", -12_050).toDomain()

        assertEquals("e1", spending.envelopeId)
        assertEquals(12_050, spending.spentCents)
    }
}
