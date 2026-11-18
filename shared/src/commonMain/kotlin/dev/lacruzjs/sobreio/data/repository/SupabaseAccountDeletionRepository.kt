package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.local.LastAccountStore
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.remote.ErrorMapper
import dev.lacruzjs.sobreio.data.remote.dto.DeletionPreviewDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText

/** Calls `account_deletion_preview` and the `delete-account` Edge Function. */
class SupabaseAccountDeletionRepository(
    private val client: SupabaseClient,
    private val lastGroup: LastGroupStore,
    private val lastAccount: LastAccountStore,
) : AccountDeletionRepository {

    override suspend fun getPreview(): Result<List<DeletionPreviewItem>> = safeCall {
        client.postgrest.rpc("account_deletion_preview")
            .decodeList<DeletionPreviewDto>()
            .map { it.toDomain() }
    }

    override suspend fun deleteAccount(): Result<Unit> = safeCall {
        val userId = client.auth.currentUserOrNull()?.id
        try {
            client.functions.invoke(FUNCTION)
        } catch (e: RestException) {
            throw functionError(e)
        }
        if (userId != null) {
            lastGroup.clear(userId)
            lastAccount.clear(userId)
        }
        // The user no longer exists, so the server cannot revoke the session: close it on this device only
        client.auth.signOut(SignOutScope.LOCAL)
    }

    /** The function answers `{"code": ..., "message": ...}`; the code decides the [AppError], as for an RPC. */
    private suspend fun functionError(e: RestException): AppError {
        val fromDescription = ErrorMapper.fromFunctionBody(e.description)
        if (fromDescription != null) return fromDescription
        val body = try {
            e.response.bodyAsText()
        } catch (_: Exception) {
            null
        }
        return ErrorMapper.fromFunctionBody(body) ?: ErrorMapper.toAppError(e)
    }

    private companion object {
        const val FUNCTION = "delete-account"
    }
}
