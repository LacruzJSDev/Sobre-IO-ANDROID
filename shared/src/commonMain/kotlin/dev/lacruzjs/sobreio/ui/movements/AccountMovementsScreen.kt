package dev.lacruzjs.sobreio.ui.movements

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
    modifier: Modifier = Modifier,
    viewModel: MovementsViewModel = koinViewModel { parametersOf(groupId, accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(MovementsEvent.Refresh) }
    AccountMovementsScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

@Composable
fun AccountMovementsScreen(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = state.fixedAccount
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
                    title = account?.name.orEmpty(),
                    subtitle = state.groupName.takeIf { it.isNotEmpty() },
                    onBack = onBack,
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
