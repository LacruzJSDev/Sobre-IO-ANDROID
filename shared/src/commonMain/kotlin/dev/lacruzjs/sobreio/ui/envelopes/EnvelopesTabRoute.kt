package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_transfers
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.TabHeader
import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Envelopes tab of the group shell: it creates its ViewModel (kept by the group's entry, so the tab keeps its state
 * while another is shown), draws the shell's [header] with the moves icon and the "+" and wires its clicks to navigation.
 */
@Composable
fun EnvelopesTabRoute(
    group: Group,
    header: TabHeader,
    onOpenEnvelopeForm: (envelopeId: String?) -> Unit,
    onOpenEnvelope: (envelopeId: String) -> Unit,
    onOpenEnvelopeTransfers: () -> Unit,
    viewModel: EnvelopesViewModel = koinViewModel { parametersOf(group.id) },
) {
    // The currency and rights reach the ViewModel before the first frame of the tab (a plain state update, safe to repeat)
    remember(viewModel, group) { viewModel.onEvent(EnvelopesEvent.GroupLoaded(group.currencyCode, group.canEdit)) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Shown again each time the tab is selected, so it reads the summary and the list again
    LaunchedEffect(Unit) { viewModel.onEvent(EnvelopesEvent.Refresh) }
    RefreshOnRemoteChange(paused = state.undoMessage != null) { viewModel.onEvent(EnvelopesEvent.Refresh) }

    header(state.isRefreshing) {
        // Every member sees the moves between envelopes; only owner and admin create envelopes
        IconActionButton(
            icon = Icons.Filled.SwapHoriz,
            label = stringResource(Res.string.envelope_detail_section_transfers),
            onClick = onOpenEnvelopeTransfers,
        )
        if (state.canEdit) {
            AddIconButton(contentDescription = stringResource(Res.string.action_new_envelope), onClick = { onOpenEnvelopeForm(null) })
        }
    }
    EnvelopesPane(
        state = state,
        onEvent = { event ->
            when (event) {
                is EnvelopesEvent.EditClicked -> onOpenEnvelopeForm(event.envelopeId)
                is EnvelopesEvent.OpenClicked -> onOpenEnvelope(event.envelopeId)
                EnvelopesEvent.TransfersClicked -> onOpenEnvelopeTransfers()
                else -> viewModel.onEvent(event)
            }
        },
    )
}
