package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.io.IOException

/** Turns any error from the backend into an [AppError]. Text for the user is chosen in the UI layer. */
object ErrorMapper {

    fun toAppError(error: Throwable): AppError = when (error) {
        is AppError -> error
        // Before any generic REST handling: AuthRestException is a RestException too
        is AuthRestException -> fromAuthErrorCode(error.errorCode) ?: AppError.Unknown(error)
        is PostgrestRestException -> fromPostgrestCode(error.code) ?: AppError.Unknown(error)
        // Ktor reports connection failures and timeouts as IOException on every platform (OkHttp, Darwin)
        is HttpRequestException, is IOException -> AppError.Network
        else -> AppError.Unknown(error)
    }

    /** The database reports business-rule violations as SQLSTATE class FZ (D-23); a few standard codes are mapped too. */
    internal fun fromPostgrestCode(code: String?): AppError? = when {
        code == null -> null
        code.startsWith("FZ") -> AppError.BusinessRule(code)
        // A row level security policy rejected the write: the user's role in the group does not allow it
        code == INSUFFICIENT_PRIVILEGE -> AppError.BusinessRule("FZ016")
        // Unique index on the name among live items of a group
        code == UNIQUE_VIOLATION -> AppError.DuplicateName
        else -> null
    }

    private const val INSUFFICIENT_PRIVILEGE = "42501"
    private const val UNIQUE_VIOLATION = "23505"

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
