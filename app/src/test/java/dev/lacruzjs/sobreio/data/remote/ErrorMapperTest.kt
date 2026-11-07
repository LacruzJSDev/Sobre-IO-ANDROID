package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test
    fun appErrorPassesThroughUnchanged() {
        val error = AppError.BusinessRule("FZ003")

        assertSame(error, ErrorMapper.toAppError(error))
    }

    @Test
    fun ioFailureMapsToNetwork() {
        assertEquals(AppError.Network, ErrorMapper.toAppError(IOException("timeout")))
    }

    @Test
    fun unexpectedExceptionMapsToUnknownKeepingCause() {
        val cause = IllegalStateException()

        assertEquals(AppError.Unknown(cause), ErrorMapper.toAppError(cause))
    }

    @Test
    fun fzCodeMapsToBusinessRule() {
        assertEquals(AppError.BusinessRule("FZ016"), ErrorMapper.fromPostgrestCode("FZ016"))
    }

    @Test
    fun nonFzOrMissingCodeIsNotABusinessRule() {
        listOf("23505", "PGRST116", null).forEach { assertNull(ErrorMapper.fromPostgrestCode(it)) }
    }
}
