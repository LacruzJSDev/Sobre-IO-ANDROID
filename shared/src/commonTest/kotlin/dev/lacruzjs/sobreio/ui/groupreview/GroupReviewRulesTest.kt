package dev.lacruzjs.sobreio.ui.groupreview

import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupReviewRulesTest {

    private val dueDate = LocalDate(2026, 11, 10)

    private fun due(kind: CommitmentKind = CommitmentKind.Expense, envelope: String? = "e1", date: LocalDate = dueDate) =
        DueOccurrence("c1", "v1", "Luz", kind, date, 6_000, "a1", null, envelope, false)

    private fun funding(envelope: String? = "e1", next: LocalDate = dueDate, reserved: Long = 6_000) = CommitmentFunding(
        envelopeId = envelope, nextDueDate = next, cycleAmountCents = 6_000, cycleStart = LocalDate(2026, 11, 1),
        reservedInCycleCents = reserved, thisPeriodCents = reserved, periodsInCycle = 1, periodsLeft = 0,
    )

    @Test
    fun theReserveOfThisDueDateIsRead() {
        assertEquals(6_000, reservedForDue(due(), funding()))
        assertEquals(6_000, reservedForDue(due(kind = CommitmentKind.GroupTransferOut), funding()))
    }

    @Test
    fun noReserveWhenItIsNotThisPaymentOrThisEnvelope() {
        assertNull(reservedForDue(due(), null))
        assertNull(reservedForDue(due(), funding(next = LocalDate(2026, 12, 10))), "an overdue due date of an earlier cycle")
        assertNull(reservedForDue(due(), funding(envelope = "e2")))
        assertNull(reservedForDue(due(kind = CommitmentKind.Income), funding()))
        assertNull(reservedForDue(due(), funding(reserved = 0)))
    }

    @Test
    fun aCheaperPaymentLeavesTheRestForTheNextReserve() {
        assertEquals(ReserveNote.Less(6_000, 1_200), reserveNoteOf(6_000, 4_800, coverShort = false))
    }

    @Test
    fun aDearerPaymentTakesFromWhatTheEnvelopeHeld() {
        assertEquals(ReserveNote.More(6_000, 1_000), reserveNoteOf(6_000, 7_000, coverShort = false))
    }

    @Test
    fun aDearerPaymentTheEnvelopeCannotCoverLeavesTheExplanationToTheCoverNote() {
        assertEquals(ReserveNote.Exact(6_000), reserveNoteOf(6_000, 7_000, coverShort = true))
    }

    @Test
    fun theExactAmountOrNoAmountYetOnlyStatesTheReserve() {
        assertEquals(ReserveNote.Exact(6_000), reserveNoteOf(6_000, 6_000, coverShort = false))
        assertEquals(ReserveNote.Exact(6_000), reserveNoteOf(6_000, null, coverShort = false))
        assertNull(reserveNoteOf(null, 4_800, coverShort = false))
    }
}
