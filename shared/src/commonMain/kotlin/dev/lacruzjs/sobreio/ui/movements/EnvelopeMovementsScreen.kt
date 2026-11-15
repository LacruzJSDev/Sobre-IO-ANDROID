package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 3 of the Envelopes tab: the movements of one envelope ("See all" in its detail). It is the same pane as the
 * Movements tab with the envelope fixed; back returns to the detail.
 */
@Composable
fun EnvelopeMovementsRoute(
    groupId: String,
    envelopeId: String,
    onBack: () -> Unit,
    onOpenMovementForm: (movementId: String?, source: MovementSource?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = koinViewModel { parametersOf(groupId, null, envelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(MovementsEvent.Refresh) }
    EnvelopeMovementsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                MovementsEvent.NewClicked -> onOpenMovementForm(null, null)
                is MovementsEvent.EditClicked -> onOpenMovementForm(event.movementId, event.source)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun EnvelopeMovementsScreen(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val envelope = state.fixedEnvelope
    // The system envelope is stored with a Spanish name: the UI has its own, translated
    val isUnassigned = envelope?.kind == EnvelopeKind.System
    val name = if (isUnassigned) stringResource(Res.string.envelopes_unassigned) else envelope?.name.orEmpty()
    FixedFilterMovementsScreen(
        state = state,
        onEvent = onEvent,
        onBack = onBack,
        title = name,
        modifier = modifier,
        leading = {
            if (envelope != null) {
                EntityAvatar(
                    name = name,
                    icon = if (isUnassigned) Icons.Filled.Inbox else iconFor(IconCategory.Envelope, envelope.icon),
                    color = EntityColor.fromHex(envelope.color),
                    size = 36.dp,
                )
            }
        },
    )
}
