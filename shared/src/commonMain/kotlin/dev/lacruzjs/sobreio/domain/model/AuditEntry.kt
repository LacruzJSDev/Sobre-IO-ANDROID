package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Instant

/** What a row of `audit_log` records (D-41): the row was created or changed. Deletes do not exist (they are voids). */
enum class AuditAction { Created, Changed }

/**
 * One row of `audit_log` for a movement: who ([changedBy], null for the daily task), when, and for a change the columns
 * that changed ([fields], the database's column names, e.g. `amount`, `date`, `voided_at`).
 */
data class AuditEntry(
    val action: AuditAction,
    val changedBy: String?,
    val changedAt: Instant,
    val fields: List<String>,
)
