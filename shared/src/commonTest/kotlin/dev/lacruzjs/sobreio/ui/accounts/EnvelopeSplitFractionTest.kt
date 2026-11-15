package dev.lacruzjs.sobreio.ui.accounts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvelopeSplitFractionTest {

    @Test
    fun splitIsHiddenWhenNothingIsOutsideTheEnvelopes() {
        assertNull(envelopeSplitFraction(inEnvelopesCents = 129815, outsideCents = 0))
    }

    @Test
    fun splitIsTheShareInTheEnvelopes() {
        assertEquals(0.75f, envelopeSplitFraction(30000, 10000))
        assertEquals(0.5f, envelopeSplitFraction(10000, 10000))
    }

    @Test
    fun anOverdrawnSideStillGetsItsShare() {
        assertEquals(0.5f, envelopeSplitFraction(10000, -10000))
    }
}
