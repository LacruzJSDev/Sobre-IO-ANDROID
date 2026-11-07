package dev.lacruzjs.sobreio.ui.error

import dev.lacruzjs.sobreio.R
import dev.lacruzjs.sobreio.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ErrorMessagesTest {

    private val raisedCodes = (1..20).map { "FZ%03d".format(it) } - setOf("FZ004", "FZ010")

    @Test
    fun everyRaisedCodeHasItsOwnMessage() {
        val messages = raisedCodes.map { AppError.BusinessRule(it).messageRes() }

        messages.forEach { assertNotEquals(R.string.error_unknown, it) }
        assertEquals(raisedCodes.size, messages.toSet().size)
    }

    @Test
    fun reservedOrUnknownCodesFallBackToGenericMessage() {
        listOf("FZ004", "FZ010", "FZ099").forEach {
            assertEquals(R.string.error_unknown, AppError.BusinessRule(it).messageRes())
        }
    }

    @Test
    fun networkAndUnknownHaveTheirMessages() {
        assertEquals(R.string.error_network, AppError.Network.messageRes())
        assertEquals(R.string.error_unknown, AppError.Unknown().messageRes())
    }

    @Test
    fun everyAuthErrorHasItsOwnMessage() {
        val messages = listOf(
            AppError.InvalidCredentials,
            AppError.EmailNotConfirmed,
            AppError.EmailAlreadyRegistered,
            AppError.WeakPassword,
            AppError.InvalidCode,
            AppError.TooManyRequests,
        ).map { it.messageRes() }

        messages.forEach { assertNotEquals(R.string.error_unknown, it) }
        assertEquals(messages.size, messages.toSet().size)
    }
}
