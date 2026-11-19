package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.ui.components.TagDot
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The movements of one tag (route `TagMovements`, opened by tapping a tag): the Movements list starting filtered by the
 * tag, so its totals and report are the tag's (D-54). Back returns to the tags.
 */
@Composable
fun TagMovementsRoute(
    groupId: String,
    tagId: String,
    onBack: () -> Unit,
    onOpenMovementForm: (movementId: String?, source: MovementSource?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = koinViewModel(key = "tag-$tagId") { parametersOf(groupId, null, null, tagId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(MovementsEvent.Refresh) }
    val tag = state.tags.firstOrNull { it.id == tagId }
    FixedFilterMovementsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                MovementsEvent.NewClicked -> onOpenMovementForm(null, null)
                is MovementsEvent.EditClicked -> onOpenMovementForm(event.movementId, event.source)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        title = tag?.name.orEmpty(),
        modifier = modifier,
        leading = { if (tag != null) TagDot(tag.color, size = 16.dp) },
    )
}
