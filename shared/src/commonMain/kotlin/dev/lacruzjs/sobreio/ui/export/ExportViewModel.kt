package dev.lacruzjs.sobreio.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.ExportRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.export.exportFileName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Exports one period of the group as a CSV file (D-36). The database gives the rows (`export_period`); this ViewModel
 * only picks the period and hands the rows to the Route, which writes the file through the platform's save dialog.
 */
class ExportViewModel(
    private val groupId: String,
    private val exports: ExportRepository,
    private val groups: GroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun onEvent(event: ExportEvent) {
        when (event) {
            ExportEvent.Opened -> open()
            ExportEvent.Dismissed -> _state.update { if (it.isExporting || it.pendingRows != null) it else ExportUiState(periods = it.periods) }
            is ExportEvent.PeriodSelected ->
                _state.update { it.copy(selectedPeriodId = event.periodId, saved = false, saveFailed = false, error = null) }
            ExportEvent.ExportClicked -> export()
            is ExportEvent.FileHandled -> _state.update {
                it.copy(
                    pendingRows = null,
                    pendingFileName = null,
                    saved = event.result == SaveResult.Saved,
                    saveFailed = event.result == SaveResult.Failed,
                )
            }
        }
    }

    private fun open() {
        _state.update { it.copy(open = true, saved = false, saveFailed = false, error = null) }
        if (_state.value.periods.isNotEmpty()) return
        _state.update { it.copy(isLoadingPeriods = true) }
        viewModelScope.launch {
            exports.getPeriods(groupId)
                .onSuccess { list ->
                    _state.update { it.copy(periods = list, selectedPeriodId = list.firstOrNull()?.id, isLoadingPeriods = false) }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingPeriods = false, error = e.asAppError()) } }
        }
    }

    private fun export() {
        val current = _state.value
        val period = current.selectedPeriod ?: return
        if (!current.canExport) return
        _state.update { it.copy(isExporting = true, saved = false, saveFailed = false, error = null) }
        viewModelScope.launch {
            val groupName = groups.getGroup(groupId).getOrNull()?.name.orEmpty()
            exports.exportPeriod(groupId, period.id)
                .onSuccess { rows ->
                    _state.update {
                        it.copy(isExporting = false, pendingRows = rows, pendingFileName = exportFileName(groupName, period))
                    }
                }
                .onFailure { e -> _state.update { it.copy(isExporting = false, error = e.asAppError()) } }
        }
    }
}
