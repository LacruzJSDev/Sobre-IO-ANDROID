package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvelopesHeroTest {

    private val day = LocalDate(2026, 11, 28)

    @Test
    fun negativeWithCoveringIncomeIsCovered() {
        val summary = EnvelopeSummary(unassignedCents = -500, coveredOn = day, coveringIncomeCents = 900)
        assertEquals(EnvelopesHeroLine.Covered(900, day), envelopesHeroLine(summary))
    }

    @Test
    fun negativeWithoutCoveringIncomeIsUncovered() {
        assertEquals(EnvelopesHeroLine.Uncovered, envelopesHeroLine(EnvelopeSummary(unassignedCents = -500)))
        // A date without an amount cannot be worded
        assertEquals(EnvelopesHeroLine.Uncovered, envelopesHeroLine(EnvelopeSummary(unassignedCents = -500, coveredOn = day)))
    }

    @Test
    fun positiveAndZeroHaveTheirOwnLines() {
        assertEquals(EnvelopesHeroLine.ToAssign, envelopesHeroLine(EnvelopeSummary(unassignedCents = 1)))
        assertEquals(EnvelopesHeroLine.AllAssigned, envelopesHeroLine(EnvelopeSummary(unassignedCents = 0)))
    }
}
