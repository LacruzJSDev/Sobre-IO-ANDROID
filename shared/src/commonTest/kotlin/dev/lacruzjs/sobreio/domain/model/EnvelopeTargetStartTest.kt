package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvelopeTargetStartTest {

    private val today = LocalDate(2026, 11, 18)
    private val envelope = Envelope("e1", "g1", "Gym", EnvelopeKind.Spending, 3_000, 0, targetsFrom = LocalDate(2026, 12, 1))

    @Test
    fun aTargetFromALaterDayStartsAfterToday() {
        assertTrue(envelope.targetStartsAfter(today))
    }

    @Test
    fun aTargetFromTodayOrEarlierHasStarted() {
        assertFalse(envelope.copy(targetsFrom = today).targetStartsAfter(today))
        assertFalse(envelope.copy(targetsFrom = LocalDate(2026, 1, 1)).targetStartsAfter(today))
    }

    @Test
    fun noTargetOrNoDatesNeverStartsLater() {
        assertFalse(envelope.copy(monthlyTargetCents = 0).targetStartsAfter(today))
        assertFalse(envelope.copy(targetsFrom = null).targetStartsAfter(today))
        assertFalse(envelope.targetStartsAfter(null))
    }

    @Test
    fun onlyManualMovesChangeEnvelopes() {
        assertTrue(TransferOrigin.Manual.canChangeEnvelopes)
        assertFalse(TransferOrigin.Target.canChangeEnvelopes)
        assertTrue(TransferOrigin.Commitment.canVoid)
        assertFalse(TransferOrigin.Commitment.canEdit)
        assertFalse(TransferOrigin.Cover.canVoid)
        assertFalse(TransferOrigin.Release.canVoid)
    }
}
