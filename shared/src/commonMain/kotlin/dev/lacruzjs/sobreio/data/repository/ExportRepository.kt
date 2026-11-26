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

    /**
     * Everything the signed-in user can see of their own, as the JSON text `export_my_data` returns (D-58): their
     * profile, legal acceptances and every row of their groups. The app saves it as it is; nothing is read from it.
     */
    suspend fun exportMyData(): Result<String>
}
