package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.AuditEntry

/**
 * The change history the database keeps by trigger (`audit_log`, D-41): read only for the group's members. Failures
 * are [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface AuditRepository {
    /** Every creation and change of the row [rowId] of [table] (`transactions`, `account_transfers`…), oldest first. */
    suspend fun getHistory(table: String, rowId: String): Result<List<AuditEntry>>
}
