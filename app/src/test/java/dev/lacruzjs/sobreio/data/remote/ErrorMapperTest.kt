package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    private val raisedCodes = (1..20).map { "FZ%03d".format(it) } - setOf("FZ004", "FZ010")

    @Test
    fun everyRaisedCodeHasItsOwnMessage() {
        val messages = raisedCodes.map { ErrorMapper.messageForCode(it) }

        messages.forEach { assertNotEquals(R.string.error_unknown, it) }
        assertEquals(raisedCodes.size, messages.toSet().size)
    }

    @Test
    fun reservedUnknownOrMissingCodesFallBackToGenericMessage() {
        listOf("FZ004", "FZ010", "FZ099", "23505", null).forEach {
            assertEquals(R.string.error_unknown, ErrorMapper.messageForCode(it))
        }
    }

    @Test
    fun ioFailureMapsToNetworkMessage() {
        assertEquals(R.string.error_network, ErrorMapper.messageFor(IOException("timeout")))
    }

    @Test
    fun unexpectedExceptionMapsToGenericMessage() {
        assertEquals(R.string.error_unknown, ErrorMapper.messageFor(IllegalStateException()))
    }
}
