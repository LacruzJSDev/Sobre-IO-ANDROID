package dev.lacruzjs.sobreio.ui.export

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.export.ExportPeriod
import dev.lacruzjs.sobreio.domain.export.ExportRow

/** The "Export a period" sheet of the Movements tab. */
data class ExportUiState(
    val open: Boolean = false,
    /** The group's periods, newest first; the newest is chosen when they arrive. */
    val periods: List<ExportPeriod> = emptyList(),
    val selectedPeriodId: String? = null,
    val isLoadingPeriods: Boolean = false,
    val isExporting: Boolean = false,
    /**
     * The rows of `export_period` waiting to be written: the Route turns them into the CSV (it holds the translated
     * texts and the platform's save dialog) and answers with [ExportEvent.FileHandled].
     */
    val pendingRows: List<ExportRow>? = null,
    val pendingFileName: String? = null,
    /** The last file reached its place. */
    val saved: Boolean = false,
    val saveFailed: Boolean = false,
    val error: AppError? = null,
) {
    val selectedPeriod: ExportPeriod? get() = periods.firstOrNull { it.id == selectedPeriodId }
    val canExport: Boolean get() = selectedPeriod != null && !isExporting && pendingRows == null
}
