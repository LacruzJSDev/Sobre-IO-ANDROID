package dev.lacruzjs.sobreio.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
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
import dev.lacruzjs.sobreio.generated.resources.ic_person_add
import dev.lacruzjs.sobreio.generated.resources.action_invite_to_group
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.action_new_movement
import dev.lacruzjs.sobreio.generated.resources.tab_accounts
import dev.lacruzjs.sobreio.generated.resources.tab_accounts_empty
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes_empty
import dev.lacruzjs.sobreio.generated.resources.tab_group
import dev.lacruzjs.sobreio.generated.resources.tab_group_empty
import dev.lacruzjs.sobreio.generated.resources.tab_movements
import dev.lacruzjs.sobreio.generated.resources.tab_movements_empty
import dev.lacruzjs.sobreio.ui.accounts.AccountsEvent
import dev.lacruzjs.sobreio.ui.accounts.AccountsPane
import dev.lacruzjs.sobreio.ui.accounts.AccountsViewModel
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.ComingSoonSheet
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.GroupAvatar
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesEvent
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesPane
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesViewModel
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
    accountsViewModel: AccountsViewModel = koinViewModel { parametersOf(groupId) },
    envelopesViewModel: EnvelopesViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Saved as the enum's position: it is the one thing the platform savers are sure to handle
    var selectedIndex by rememberSaveable { mutableStateOf(GroupTab.Group.ordinal) }

    val accountsState by accountsViewModel.state.collectAsStateWithLifecycle()
    // Currency and role of the group reach the accounts tab once the group has loaded, before its content is shown
    val envelopesState by envelopesViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.group) {
        state.group?.let {
            accountsViewModel.onEvent(AccountsEvent.GroupLoaded(it.currencyCode, it.canEdit))
            envelopesViewModel.onEvent(EnvelopesEvent.GroupLoaded(it.currencyCode, it.canEdit))
        }
    }

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
        accountsAction = {
            if (accountsState.canEdit) {
                AddIconButton(
                    contentDescription = stringResource(Res.string.action_new_account),
                    onClick = { accountsViewModel.onEvent(AccountsEvent.NewAccountClicked) },
                )
            }
        },
        accountsContent = {
            // Shown again each time the tab is selected, so it reads the list again
            LaunchedEffect(Unit) { accountsViewModel.onEvent(AccountsEvent.Refresh) }
            AccountsPane(state = accountsState, onEvent = accountsViewModel::onEvent)
        },
        envelopesAction = {
            if (envelopesState.canEdit) {
                AddIconButton(
                    contentDescription = stringResource(Res.string.action_new_envelope),
                    onClick = { envelopesViewModel.onEvent(EnvelopesEvent.NewEnvelopeClicked) },
                )
            }
        },
        envelopesContent = {
            // Shown again each time the tab is selected, so it reads the summary and the list again
            LaunchedEffect(Unit) { envelopesViewModel.onEvent(EnvelopesEvent.Refresh) }
            EnvelopesPane(state = envelopesState, onEvent = envelopesViewModel::onEvent)
        },
    )
}

/**
 * The group's four sections: a bar on phones, a rail on wider windows (what NavigationSuiteScaffold picks). The Accounts
 * and Envelopes tabs have their own state and ViewModel, so the shell takes their header action and content as slots.
 */
@Composable
fun GroupScreen(
    state: GroupUiState,
    onEvent: (GroupEvent) -> Unit,
    modifier: Modifier = Modifier,
    accountsAction: @Composable () -> Unit = {},
    accountsContent: @Composable () -> Unit = { EmptyState(message = stringResource(Res.string.tab_accounts_empty)) },
    envelopesAction: @Composable () -> Unit = {},
    envelopesContent: @Composable () -> Unit = { EmptyState(message = stringResource(Res.string.tab_envelopes_empty)) },
) {
    if (state.newItemSheetOpen) {
        ComingSoonSheet(
            title = stringResource(state.selectedTab.newItemLabel()),
            onDismiss = { onEvent(GroupEvent.NewItemDismissed) },
        )
    }
    if (state.inviteSheetOpen) InviteSheet(state, onEvent)

    val selected = MaterialTheme.colorScheme.primary
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = selected,
            indicatorColor = selected,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = selected,
            indicatorColor = selected,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.background,
            navigationRailContainerColor = MaterialTheme.colorScheme.background,
            navigationDrawerContainerColor = MaterialTheme.colorScheme.background,
        ),
        navigationSuiteItems = {
            GroupTab.entries.forEach { tab ->
                item(
                    selected = tab == state.selectedTab,
                    onClick = { onEvent(GroupEvent.TabSelected(tab)) },
                    icon = { Icon(painter = painterResource(tab.icon()), contentDescription = null) },
                    label = { Text(stringResource(tab.label())) },
                    colors = itemColors,
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
                    actions = { HeaderAction(state, onEvent, accountsAction, envelopesAction) },
                )
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(GroupEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
                    }
                    state.selectedTab == GroupTab.Accounts -> accountsContent()
                    state.selectedTab == GroupTab.Envelopes -> envelopesContent()
                    else -> EmptyState(message = stringResource(state.selectedTab.emptyMessage()))
                }
            }
        }
    }
}

/** Right side of the header: Invite on the Group tab (owner and admin), "+" on the lists. */
@Composable
private fun HeaderAction(
    state: GroupUiState,
    onEvent: (GroupEvent) -> Unit,
    accountsAction: @Composable () -> Unit,
    envelopesAction: @Composable () -> Unit,
) {
    when (state.selectedTab) {
        GroupTab.Group -> if (state.group?.canEdit == true) {
            IconButton(onClick = { onEvent(GroupEvent.InviteClicked) }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_person_add),
                    contentDescription = stringResource(Res.string.action_invite_to_group),
                )
            }
        }
        GroupTab.Accounts -> accountsAction()
        GroupTab.Envelopes -> envelopesAction()
        GroupTab.Movements -> AddIconButton(
            contentDescription = stringResource(state.selectedTab.newItemLabel()),
            onClick = { onEvent(GroupEvent.NewItemClicked) },
        )
    }
}

private fun GroupTab.newItemLabel(): StringResource = when (this) {
    GroupTab.Accounts -> Res.string.action_new_account
    GroupTab.Envelopes -> Res.string.action_new_envelope
    else -> Res.string.action_new_movement
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
