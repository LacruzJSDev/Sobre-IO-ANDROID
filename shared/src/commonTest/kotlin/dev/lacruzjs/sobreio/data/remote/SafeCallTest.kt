package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SafeCallTest {

    @Test
    fun successReturnsTheValue() = runTest {
        assertEquals(Result.success(42), safeCall { 42 })
    }

    @Test
    fun failureIsMappedToAppError() = runTest {
        val result = safeCall<Unit> { throw IOException("offline") }

        assertEquals(AppError.Network, result.exceptionOrNull())
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        try {
            safeCall<Unit> { throw CancellationException("cancelled") }
            fail("CancellationException must propagate")
        } catch (e: CancellationException) {
            assertTrue(e.message == "cancelled")
        }
    }
}
