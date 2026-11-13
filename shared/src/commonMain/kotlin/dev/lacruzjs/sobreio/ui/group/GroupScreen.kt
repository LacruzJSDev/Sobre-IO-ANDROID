package dev.lacruzjs.sobreio.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.ic_account_balance
import dev.lacruzjs.sobreio.generated.resources.ic_envelope
import dev.lacruzjs.sobreio.generated.resources.ic_home
import dev.lacruzjs.sobreio.generated.resources.ic_list
import dev.lacruzjs.sobreio.generated.resources.tab_accounts
import dev.lacruzjs.sobreio.generated.resources.tab_accounts_empty
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes_empty
import dev.lacruzjs.sobreio.generated.resources.tab_group
import dev.lacruzjs.sobreio.generated.resources.tab_group_empty
import dev.lacruzjs.sobreio.generated.resources.tab_movements
import dev.lacruzjs.sobreio.generated.resources.tab_movements_empty
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.GroupAvatar
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupRoute(
    groupId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Saved as the enum's position: it is the one thing the platform savers are sure to handle
    var selectedIndex by rememberSaveable { mutableStateOf(GroupTab.Group.ordinal) }

    if (state.navigateBack) {
        LaunchedEffect(Unit) {
            onBack()
            viewModel.onEvent(GroupEvent.NavigationHandled)
        }
    }

    GroupScreen(
        state = state.copy(selectedTab = GroupTab.entries[selectedIndex]),
        onEvent = { event ->
            when (event) {
                GroupEvent.BackClicked -> onBack()
                is GroupEvent.TabSelected -> selectedIndex = event.tab.ordinal
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/** The group's four sections: a bar on phones, a rail on wider windows (what NavigationSuiteScaffold picks). */
@Composable
fun GroupScreen(
    state: GroupUiState,
    onEvent: (GroupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            GroupTab.entries.forEach { tab ->
                item(
                    selected = tab == state.selectedTab,
                    onClick = { onEvent(GroupEvent.TabSelected(tab)) },
                    icon = { Icon(painter = painterResource(tab.icon()), contentDescription = null) },
                    label = { Text(stringResource(tab.label())) },
                )
            }
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Same header on every tab. The right side is free for the actions of each section.
                ScreenHeader(
                    title = state.group?.name.orEmpty(),
                    onBack = { onEvent(GroupEvent.BackClicked) },
                    leading = { GroupAvatar(name = state.group?.name.orEmpty(), size = 36.dp) },
                )
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(GroupEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
                    }
                    else -> EmptyState(message = stringResource(state.selectedTab.emptyMessage()))
                }
            }
        }
    }
}

private fun GroupTab.icon(): DrawableResource = when (this) {
    GroupTab.Group -> Res.drawable.ic_home
    GroupTab.Accounts -> Res.drawable.ic_account_balance
    GroupTab.Envelopes -> Res.drawable.ic_envelope
    GroupTab.Movements -> Res.drawable.ic_list
}

private fun GroupTab.label(): StringResource = when (this) {
    GroupTab.Group -> Res.string.tab_group
    GroupTab.Accounts -> Res.string.tab_accounts
    GroupTab.Envelopes -> Res.string.tab_envelopes
    GroupTab.Movements -> Res.string.tab_movements
}

private fun GroupTab.emptyMessage(): StringResource = when (this) {
    GroupTab.Group -> Res.string.tab_group_empty
    GroupTab.Accounts -> Res.string.tab_accounts_empty
    GroupTab.Envelopes -> Res.string.tab_envelopes_empty
    GroupTab.Movements -> Res.string.tab_movements_empty
}

@PreviewLightDark
@Composable
private fun GroupScreenPreview() {
    SobreioTheme {
        GroupScreen(
            state = GroupUiState(group = Group("1", "Casa", "EUR"), isLoading = false, selectedTab = GroupTab.Accounts),
            onEvent = {},
        )
    }
}
