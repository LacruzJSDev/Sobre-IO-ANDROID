package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.export.ExportRow

/**
 * Exports of a whole period (D-36). The rows come from `export_period`, under the same RLS as everything else: each
 * person only exports their groups. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. Not cached: an export
 * is always read fresh.
 */
interface ExportRepository {

    /** Every live movement and move between envelopes of the period, ordered by date (`export_period`). */
    suspend fun exportPeriod(groupId: String, periodId: String): Result<List<ExportRow>>
}
