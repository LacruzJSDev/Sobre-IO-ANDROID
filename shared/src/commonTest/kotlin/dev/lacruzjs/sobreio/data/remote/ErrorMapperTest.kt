package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

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
        // A row level security rejection means the role does not allow it
        assertEquals(AppError.BusinessRule("FZ016"), ErrorMapper.fromPostgrestCode("42501"))
    }

    @Test
    fun nonFzOrMissingCodeIsNotABusinessRule() {
        listOf("23505", "PGRST116", null).forEach { assertNull(ErrorMapper.fromPostgrestCode(it)) }
    }

    @Test
    fun authErrorCodesMapToTheirAppErrors() {
        val expected = mapOf(
            AuthErrorCode.InvalidCredentials to AppError.InvalidCredentials,
            AuthErrorCode.EmailNotConfirmed to AppError.EmailNotConfirmed,
            AuthErrorCode.UserAlreadyExists to AppError.EmailAlreadyRegistered,
            AuthErrorCode.EmailExists to AppError.EmailAlreadyRegistered,
            AuthErrorCode.WeakPassword to AppError.WeakPassword,
            AuthErrorCode.OtpExpired to AppError.InvalidCode,
            AuthErrorCode.OverRequestRateLimit to AppError.TooManyRequests,
            AuthErrorCode.OverEmailSendRateLimit to AppError.TooManyRequests,
        )

        expected.forEach { (code, error) -> assertEquals(error, ErrorMapper.fromAuthErrorCode(code)) }
    }

    @Test
    fun otherOrMissingAuthCodesAreNotMapped() {
        assertNull(ErrorMapper.fromAuthErrorCode(AuthErrorCode.EmailAddressInvalid))
        assertNull(ErrorMapper.fromAuthErrorCode(null))
    }
}
