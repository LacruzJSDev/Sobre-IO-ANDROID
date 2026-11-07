package dev.lacruzjs.sobreio.ui.error

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_network
import dev.lacruzjs.sobreio.generated.resources.error_unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ErrorMessagesTest {

    private val raisedCodes = (1..20).map { "FZ${it.toString().padStart(3, '0')}" } - setOf("FZ004", "FZ010")

    @Test
    fun everyRaisedCodeHasItsOwnMessage() {
        val messages = raisedCodes.map { AppError.BusinessRule(it).messageRes() }

        messages.forEach { assertNotEquals(Res.string.error_unknown, it) }
        assertEquals(raisedCodes.size, messages.toSet().size)
    }

    @Test
    fun reservedOrUnknownCodesFallBackToGenericMessage() {
        listOf("FZ004", "FZ010", "FZ099").forEach {
            assertEquals(Res.string.error_unknown, AppError.BusinessRule(it).messageRes())
        }
    }

    @Test
    fun networkAndUnknownHaveTheirMessages() {
        assertEquals(Res.string.error_network, AppError.Network.messageRes())
        assertEquals(Res.string.error_unknown, AppError.Unknown().messageRes())
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

        messages.forEach { assertNotEquals(Res.string.error_unknown, it) }
        assertEquals(messages.size, messages.toSet().size)
    }
}
