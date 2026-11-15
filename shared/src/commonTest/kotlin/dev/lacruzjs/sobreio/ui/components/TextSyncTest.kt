package dev.lacruzjs.sobreio.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextSyncTest {
    @Test
    fun acknowledgesInOrder() {
        val sync = TextSync("")
        assertTrue(sync.onFieldText("a"))
        assertTrue(sync.onFieldText("ab"))
        assertNull(sync.onOutsideValue("a"))
        assertNull(sync.onOutsideValue("ab"))
        assertNull(sync.onOutsideValue("ab"))
    }

    @Test
    fun staleValueAfterNewerAcknowledgementIsIgnored() {
        val sync = TextSync("")
        sync.onFieldText("a")
        sync.onFieldText("ab")
        sync.onFieldText("abc")
        assertNull(sync.onOutsideValue("ab"))
        assertNull(sync.onOutsideValue("a"))
        assertNull(sync.onOutsideValue("abc"))
    }

    @Test
    fun rapidTypingWithLateFirstValue() {
        val sync = TextSync("")
        sync.onFieldText("a")
        sync.onFieldText("ab")
        sync.onFieldText("abc")
        assertNull(sync.onOutsideValue("a"))
        assertNull(sync.onOutsideValue("abc"))
    }

    @Test
    fun externalValueIsApplied() {
        val sync = TextSync("")
        assertEquals("Maria", sync.onOutsideValue("Maria"))
        assertNull(sync.onOutsideValue("Maria"))
        assertFalse(sync.onFieldText("Maria"))
        assertEquals("", sync.onOutsideValue(""))
    }

    @Test
    fun externalValueWhileTypingWins() {
        val sync = TextSync("")
        sync.onFieldText("a")
        assertEquals("x", sync.onOutsideValue("x"))
        assertNull(sync.onOutsideValue("x"))
    }

    @Test
    fun sameValueIsNoOp() {
        val sync = TextSync("abc")
        assertNull(sync.onOutsideValue("abc"))
        assertFalse(sync.onFieldText("abc"))
    }
}
