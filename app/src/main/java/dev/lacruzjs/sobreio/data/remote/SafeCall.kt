package dev.lacruzjs.sobreio.data.remote

import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and returns its value, or a failure holding an [dev.lacruzjs.sobreio.domain.error.AppError].
 * Every repository implementation wraps its calls with it, so a failed Result always holds an AppError.
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    // Swallowing it would keep a cancelled coroutine running
    throw e
} catch (e: Exception) {
    Result.failure(ErrorMapper.toAppError(e))
}
