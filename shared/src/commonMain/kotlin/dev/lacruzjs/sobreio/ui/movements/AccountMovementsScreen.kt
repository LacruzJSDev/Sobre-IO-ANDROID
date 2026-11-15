package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.model.MovementSource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 3 of the Accounts tab: the movements of one account ("See all" in its detail). It is the same pane as the
 * Movements tab with the account fixed; back returns to the detail.
 */
@Composable
fun AccountMovementsRoute(
    groupId: String,
    accountId: String,
    onBack: () -> Unit,
    onOpenMovementForm: (movementId: String?, source: MovementSource?, accountId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = koinViewModel { parametersOf(groupId, accountId, null) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(MovementsEvent.Refresh) }
    AccountMovementsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                MovementsEvent.NewClicked -> onOpenMovementForm(null, null, accountId)
                is MovementsEvent.EditClicked -> onOpenMovementForm(event.movementId, event.source, null)
                else -> viewModel.onEvent(event)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun AccountMovementsScreen(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = state.fixedAccount
    FixedFilterMovementsScreen(
        state = state,
        onEvent = onEvent,
        onBack = onBack,
        title = account?.name.orEmpty(),
        modifier = modifier,
        leading = {
            if (account != null) {
                EntityAvatar(
                    name = account.name,
                    icon = iconFor(IconCategory.Account, account.icon),
                    color = EntityColor.fromHex(account.color),
                    size = 36.dp,
                )
            }
        },
    )
}

/**
 * A Movements list whose account or envelope is fixed by the screen that opened it: the header names that item (with the
 * group as a small subtitle), has search and "+", and the pane below has the filter chips and the rows. Shared by
 * [AccountMovementsScreen] and [EnvelopeMovementsScreen].
 */
@Composable
internal fun FixedFilterMovementsScreen(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.searchOpen) {
                MovementsSearchHeader(
                    text = state.searchText,
                    onTextChange = { onEvent(MovementsEvent.SearchChanged(it)) },
                    onClose = { onEvent(MovementsEvent.SearchClosed) },
                )
            } else {
                ScreenHeader(
                    title = title,
                    subtitle = state.groupName.takeIf { it.isNotEmpty() },
                    onBack = onBack,
                    leading = leading,
                    actions = {
                        MovementsHeaderActions(
                            onSearch = { onEvent(MovementsEvent.SearchOpened) },
                            onNew = { onEvent(MovementsEvent.NewClicked) },
                        )
                    },
                )
            }
            MovementsPane(state = state, onEvent = onEvent, modifier = Modifier.navigationBarsPadding())
        }
    }
}
