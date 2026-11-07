package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException

/** Turns any error from the backend into an [AppError]. Text for the user is chosen in the UI layer. */
object ErrorMapper {

    fun toAppError(error: Throwable): AppError = when (error) {
        is AppError -> error
        // Before any generic REST handling: AuthRestException is a RestException too
        is AuthRestException -> fromAuthErrorCode(error.errorCode) ?: AppError.Unknown(error)
        is PostgrestRestException -> fromPostgrestCode(error.code) ?: AppError.Unknown(error)
        is HttpRequestException, is IOException -> AppError.Network
        else -> AppError.Unknown(error)
    }

    /** The database reports business-rule violations as SQLSTATE class FZ (D-23); other codes are not ours. */
    internal fun fromPostgrestCode(code: String?): AppError.BusinessRule? =
        code?.takeIf { it.startsWith("FZ") }?.let(AppError::BusinessRule)

    /** Auth failures that need their own message; null means "not one of ours", so the caller falls back to Unknown. */
    internal fun fromAuthErrorCode(code: AuthErrorCode?): AppError? = when (code) {
        AuthErrorCode.InvalidCredentials -> AppError.InvalidCredentials
        AuthErrorCode.EmailNotConfirmed -> AppError.EmailNotConfirmed
        AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AppError.EmailAlreadyRegistered
        AuthErrorCode.WeakPassword -> AppError.WeakPassword
        // GoTrue answers a wrong code and an expired one the same way
        AuthErrorCode.OtpExpired -> AppError.InvalidCode
        AuthErrorCode.OverRequestRateLimit, AuthErrorCode.OverEmailSendRateLimit -> AppError.TooManyRequests
        else -> null
    }
}
