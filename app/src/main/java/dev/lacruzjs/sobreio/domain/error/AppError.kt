package dev.lacruzjs.sobreio.domain.error

/**
 * Every failure the data layer can report. The UI turns it into text; nothing here knows about screens.
 * Auth-specific cases (invalid credentials, invalid code, email in use, weak password, rate limited) are added
 * with the auth feature.
 */
sealed class AppError : Exception() {

    /** The server could not be reached. */
    data object Network : AppError()

    /** A business rule of the database was violated; [code] is its SQLSTATE `FZxxx` (D-23). */
    data class BusinessRule(val code: String) : AppError()

    data class Unknown(override val cause: Throwable? = null) : AppError()
}
