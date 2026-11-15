package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvelopesHeroTest {

    private val day = LocalDate(2026, 11, 28)

    @Test
    fun negativeWithLaterMoneyAndIncomeNamesTheIncome() {
        val summary = EnvelopeSummary(unassignedCents = -500, reservedLaterCents = 300, coveredOn = day, coveredBy = "Payroll")
        assertEquals(EnvelopesHeroLine.LaterCovered(300, "Payroll", day), envelopesHeroLine(summary))
    }

    @Test
    fun negativeWithLaterMoneyAndNoIncomeSaysNothingCoversIt() {
        val summary = EnvelopeSummary(unassignedCents = -500, reservedLaterCents = 300)
        assertEquals(EnvelopesHeroLine.LaterUncovered(300), envelopesHeroLine(summary))
    }

    @Test
    fun negativeWithoutLaterMoneyButWithIncomeIsCovered() {
        val summary = EnvelopeSummary(unassignedCents = -500, coveredOn = day, coveredBy = "Payroll")
        assertEquals(EnvelopesHeroLine.Covered("Payroll", day), envelopesHeroLine(summary))
    }

    @Test
    fun otherwiseNegativeIsShortByThePositiveDeficit() {
        assertEquals(EnvelopesHeroLine.Short(500), envelopesHeroLine(EnvelopeSummary(unassignedCents = -500)))
        // A date without a name cannot be worded, so it falls back to the deficit
        assertEquals(EnvelopesHeroLine.Short(500), envelopesHeroLine(EnvelopeSummary(unassignedCents = -500, coveredOn = day)))
    }

    @Test
    fun positiveAndZeroHaveTheirOwnLines() {
        assertEquals(EnvelopesHeroLine.ToAssign, envelopesHeroLine(EnvelopeSummary(unassignedCents = 1)))
        assertEquals(EnvelopesHeroLine.AllAssigned, envelopesHeroLine(EnvelopeSummary(unassignedCents = 0)))
    }

    @Test
    fun segmentFractionsAreTheShareOfTheirSum() {
        assertEquals(listOf(0.5f, 0.25f, 0.25f), heroSegmentFractions(200, 100, 100))
        assertEquals(listOf(0f, 1f, 0f), heroSegmentFractions(0, 70, 0))
    }

    @Test
    fun segmentFractionsAreNullWhenThereIsNothing() {
        assertNull(heroSegmentFractions(0, 0, 0))
    }
}
