package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem

/** Deleting the signed-in user's own account (D-51). Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface AccountDeletionRepository {

    /** What would happen to each group of the user, blocked ones first, then deleted, then left. */
    suspend fun getPreview(): Result<List<DeletionPreviewItem>>

    /**
     * Deletes the account (Edge Function `delete-account`). On success the session is closed on this device and the
     * user's remembered group and accounts are forgotten, so the root navigation shows the sign-in screens. Fails with a
     * business rule when a group blocks it; retrying after a server failure is safe.
     */
    suspend fun deleteAccount(): Result<Unit>
}
