package dev.lacruzjs.sobreio.domain.error

/**
 * Every failure the data layer can report. The UI turns it into text; nothing here knows about screens.
 */
sealed class AppError : Exception() {

    /** The server could not be reached. */
    data object Network : AppError()

    /** A business rule of the database was violated; [code] is its SQLSTATE `FZxxx` (D-23). */
    data class BusinessRule(val code: String) : AppError()

    /** Wrong email or password. */
    data object InvalidCredentials : AppError()

    /** The account exists but its email was never confirmed with the sign-up code. */
    data object EmailNotConfirmed : AppError()

    data object EmailAlreadyRegistered : AppError()

    data object WeakPassword : AppError()

    /** The 6-digit code is wrong or expired. */
    data object InvalidCode : AppError()

    /** Too many attempts or emails in a short time. */
    data object TooManyRequests : AppError()

    data class Unknown(override val cause: Throwable? = null) : AppError()
}

/** [safeCall][dev.lacruzjs.sobreio.data.remote.safeCall] only produces [AppError]s; this keeps ViewModels free of casts. */
fun Throwable.asAppError(): AppError = this as? AppError ?: AppError.Unknown(this)
