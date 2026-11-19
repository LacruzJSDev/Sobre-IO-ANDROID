package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AuditRepository
import dev.lacruzjs.sobreio.domain.model.AuditEntry

/** `audit_log` for tests: [entries] answers every row; set [result] to script a failure. [calls] records what was read. */
class FakeAuditRepository(var entries: List<AuditEntry> = emptyList()) : AuditRepository {

    val calls = mutableListOf<String>()

    var result: Result<List<AuditEntry>>? = null

    override suspend fun getHistory(table: String, rowId: String): Result<List<AuditEntry>> {
        calls += "getHistory($table, $rowId)"
        return result ?: Result.success(entries)
    }
}
