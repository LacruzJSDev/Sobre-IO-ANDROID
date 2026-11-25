package dev.lacruzjs.sobreio.ui.auth.google

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialCancellationTest {

    @Test
    fun closingThePickerIsACancellation() {
        assertTrue(isUserCancellation("[16] Cancelled by user."))
        assertTrue(isUserCancellation("activity is cancelled by the user."))
        assertTrue(isUserCancellation("Canceled by user"))
    }

    @Test
    fun noMessageIsACancellation() {
        assertTrue(isUserCancellation(null))
        assertTrue(isUserCancellation(" "))
    }

    @Test
    fun anUnregisteredSigningKeyIsAnError() {
        assertFalse(isUserCancellation("[16] Account reauth failed."))
    }

    @Test
    fun anyOtherMessageIsAnError() {
        assertFalse(isUserCancellation("[8] Unknown error"))
    }
}
