package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.ui.components.TabHeader
import dev.lacruzjs.sobreio.ui.export.ExportEvent
import dev.lacruzjs.sobreio.ui.export.ExportHost
import dev.lacruzjs.sobreio.ui.export.ExportViewModel
import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Movements tab of the group shell: the whole ledger. It creates its ViewModel (kept by the group's entry, so the
 * filters stay while another tab is shown) and the export sheet's, draws the shell's [header] with search, export and "+"
 * (or the search field instead of the header while searching) and wires its clicks to navigation.
 */
@Composable
fun MovementsTabRoute(
    group: Group,
    header: TabHeader,
    onOpenMovementForm: (movementId: String?, source: MovementSource?) -> Unit,
    viewModel: MovementsViewModel = koinViewModel { parametersOf(group.id, null, null) },
    exportViewModel: ExportViewModel = koinViewModel { parametersOf(group.id) },
) {
    ExportHost(exportViewModel)
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Shown again each time the tab is selected, so it reads the ledger again; the filters are kept
    LaunchedEffect(Unit) { viewModel.onEvent(MovementsEvent.Refresh) }
    RefreshOnRemoteChange(paused = state.undoMessage != null) { viewModel.onEvent(MovementsEvent.Refresh) }

    if (state.searchOpen) {
        MovementsSearchHeader(
            text = state.searchText,
            onTextChange = { viewModel.onEvent(MovementsEvent.SearchChanged(it)) },
            onClose = { viewModel.onEvent(MovementsEvent.SearchClosed) },
            isRefreshing = state.isRefreshing,
        )
    } else {
        header(state.isRefreshing) {
            MovementsHeaderActions(
                onSearch = { viewModel.onEvent(MovementsEvent.SearchOpened) },
                onNew = { onOpenMovementForm(null, null) },
                onExport = { exportViewModel.onEvent(ExportEvent.Opened) },
            )
        }
    }
    MovementsPane(
        state = state,
        onEvent = { event ->
            when (event) {
                is MovementsEvent.EditClicked -> onOpenMovementForm(event.movementId, event.source)
                else -> viewModel.onEvent(event)
            }
        },
    )
}
