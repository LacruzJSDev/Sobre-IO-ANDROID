package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.domain.error.AppError
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException

/** Turns any error from the backend into an [AppError]. Text for the user is chosen in the UI layer. */
object ErrorMapper {

    fun toAppError(error: Throwable): AppError = when (error) {
        is AppError -> error
        is PostgrestRestException -> fromPostgrestCode(error.code) ?: AppError.Unknown(error)
        is HttpRequestException, is IOException -> AppError.Network
        else -> AppError.Unknown(error)
    }

    /** The database reports business-rule violations as SQLSTATE class FZ (D-23); other codes are not ours. */
    internal fun fromPostgrestCode(code: String?): AppError.BusinessRule? =
        code?.takeIf { it.startsWith("FZ") }?.let(AppError::BusinessRule)
}
