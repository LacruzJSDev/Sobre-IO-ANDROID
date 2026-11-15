package dev.lacruzjs.sobreio.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import dev.lacruzjs.sobreio.generated.resources.group_settings_title
import dev.lacruzjs.sobreio.generated.resources.ic_settings
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.action_new_movement
import dev.lacruzjs.sobreio.generated.resources.tab_accounts
import dev.lacruzjs.sobreio.generated.resources.tab_accounts_empty
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes
import dev.lacruzjs.sobreio.generated.resources.tab_envelopes_empty
import dev.lacruzjs.sobreio.generated.resources.tab_group
import dev.lacruzjs.sobreio.generated.resources.tab_group_badge
import dev.lacruzjs.sobreio.generated.resources.tab_movements
import dev.lacruzjs.sobreio.generated.resources.tab_movements_empty
import dev.lacruzjs.sobreio.ui.accounts.AccountsEvent
import dev.lacruzjs.sobreio.ui.accounts.AccountsPane
import dev.lacruzjs.sobreio.ui.accounts.AccountsViewModel
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.ComingSoonSheet
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.components.MenuButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesEvent
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesPane
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesViewModel
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryEvent
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryPane
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryViewModel
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.ui.movements.MovementsEvent
import dev.lacruzjs.sobreio.ui.movements.MovementsHeaderActions
import dev.lacruzjs.sobreio.ui.movements.MovementsPane
import dev.lacruzjs.sobreio.ui.movements.MovementsSearchHeader
import dev.lacruzjs.sobreio.ui.movements.MovementsViewModel
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
    onOpenGroups: () -> Unit,
    onGroupGone: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenAccountForm: (accountId: String?) -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
    onOpenEnvelopeForm: (envelopeId: String?) -> Unit,
    onOpenEnvelope: (envelopeId: String) -> Unit,
    onOpenMovementForm: (movementId: String?, source: MovementSource?) -> Unit,
    onOpenTransferForm: (TransferSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId) },
    accountsViewModel: AccountsViewModel = koinViewModel { parametersOf(groupId) },
    envelopesViewModel: EnvelopesViewModel = koinViewModel { parametersOf(groupId) },
    movementsViewModel: MovementsViewModel = koinViewModel { parametersOf(groupId, null, null) },
    summaryViewModel: GroupSummaryViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Saved as the enum's position: it is the one thing the platform savers are sure to handle
    var selectedIndex by rememberSaveable { mutableStateOf(GroupTab.Group.ordinal) }

    val accountsState by accountsViewModel.state.collectAsStateWithLifecycle()
    // Currency and role of the group reach the accounts tab once the group has loaded, before its content is shown
    val envelopesState by envelopesViewModel.state.collectAsStateWithLifecycle()
    val movementsState by movementsViewModel.state.collectAsStateWithLifecycle()
    val summaryState by summaryViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.group) {
        state.group?.let {
            accountsViewModel.onEvent(AccountsEvent.GroupLoaded(it.currencyCode, it.canEdit))
            envelopesViewModel.onEvent(EnvelopesEvent.GroupLoaded(it.currencyCode, it.canEdit))
            // Any member may review a due date, but not in an archived group or one pending deletion
            summaryViewModel.onEvent(GroupSummaryEvent.GroupLoaded(it.currencyCode, !it.isArchived && !it.isPendingDeletion))
        }
    }

    // Back from the group settings: the group may have been renamed, archived or left
    LaunchedEffect(Unit) {
        viewModel.onEvent(GroupEvent.Refresh)
        // The badge of the Group tab counts the due dates to review, whichever tab is open
        summaryViewModel.onEvent(GroupSummaryEvent.Refresh)
    }

    if (state.navigateBack) {
        LaunchedEffect(Unit) {
            onGroupGone()
            viewModel.onEvent(GroupEvent.NavigationHandled)
        }
    }

    GroupScreen(
        state = state.copy(selectedTab = GroupTab.entries[selectedIndex], pendingCount = summaryState.pendingCount),
        onEvent = { event ->
            when (event) {
                GroupEvent.MenuClicked -> onOpenGroups()
                GroupEvent.SettingsClicked -> onOpenSettings()
                is GroupEvent.TabSelected -> {
                    selectedIndex = event.tab.ordinal
                    // Shown again each time the tab is selected, so it reads the due dates again
                    if (event.tab == GroupTab.Group) summaryViewModel.onEvent(GroupSummaryEvent.Refresh)
                }
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
        groupContent = {
            GroupSummaryPane(
                state = summaryState,
                onEvent = { event ->
                    when (event) {
                        is GroupSummaryEvent.WarningClicked -> when (val warning = event.warning) {
                            is GroupWarning.EnvelopeOverspent -> onOpenEnvelope(warning.envelopeId)
                            is GroupWarning.AccountShortfallOn -> onOpenAccount(warning.accountId)
                            is GroupWarning.CommitmentsShortfall -> selectedIndex = GroupTab.Envelopes.ordinal
                        }
                        is GroupSummaryEvent.TransferClicked -> onOpenTransferForm(event.suggestion)
                        GroupSummaryEvent.ReviewClicked -> onOpenReview()
                        is GroupSummaryEvent.SpendingClicked -> onOpenEnvelope(event.envelopeId)
                        GroupSummaryEvent.SeeAllEnvelopesClicked -> selectedIndex = GroupTab.Envelopes.ordinal
                        else -> summaryViewModel.onEvent(event)
                    }
                },
            )
        },
        accountsAction = {
            if (accountsState.canEdit) {
                AddIconButton(
                    contentDescription = stringResource(Res.string.action_new_account),
                    onClick = { onOpenAccountForm(null) },
                )
            }
        },
        accountsContent = {
            // Shown again each time the tab is selected, so it reads the list again
            LaunchedEffect(Unit) { accountsViewModel.onEvent(AccountsEvent.Refresh) }
            AccountsPane(
                state = accountsState,
                onEvent = { event ->
                    when (event) {
                        is AccountsEvent.EditClicked -> onOpenAccountForm(event.accountId)
                        is AccountsEvent.OpenClicked -> onOpenAccount(event.accountId)
                        else -> accountsViewModel.onEvent(event)
                    }
                },
            )
        },
        envelopesAction = {
            if (envelopesState.canEdit) {
                AddIconButton(
                    contentDescription = stringResource(Res.string.action_new_envelope),
                    onClick = { onOpenEnvelopeForm(null) },
                )
            }
        },
        envelopesContent = {
            // Shown again each time the tab is selected, so it reads the summary and the list again
            LaunchedEffect(Unit) { envelopesViewModel.onEvent(EnvelopesEvent.Refresh) }
            EnvelopesPane(
                state = envelopesState,
                onEvent = { event ->
                    when (event) {
                        is EnvelopesEvent.EditClicked -> onOpenEnvelopeForm(event.envelopeId)
                        is EnvelopesEvent.OpenClicked -> onOpenEnvelope(event.envelopeId)
                        else -> envelopesViewModel.onEvent(event)
                    }
                },
            )
        },
        movementsAction = {
            MovementsHeaderActions(
                onSearch = { movementsViewModel.onEvent(MovementsEvent.SearchOpened) },
                onNew = { onOpenMovementForm(null, null) },
            )
        },
        // While searching the whole header becomes the search field
        movementsHeader = if (movementsState.searchOpen) {
            {
                MovementsSearchHeader(
                    text = movementsState.searchText,
                    onTextChange = { movementsViewModel.onEvent(MovementsEvent.SearchChanged(it)) },
                    onClose = { movementsViewModel.onEvent(MovementsEvent.SearchClosed) },
                )
            }
        } else {
            null
        },
        movementsContent = {
            // Shown again each time the tab is selected, so it reads the ledger again; the filters are kept
            LaunchedEffect(Unit) { movementsViewModel.onEvent(MovementsEvent.Refresh) }
            MovementsPane(
                state = movementsState,
                onEvent = { event ->
                    when (event) {
                        is MovementsEvent.EditClicked -> onOpenMovementForm(event.movementId, event.source)
                        else -> movementsViewModel.onEvent(event)
                    }
                },
            )
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
    groupContent: @Composable () -> Unit = {},
    accountsAction: @Composable () -> Unit = {},
    accountsContent: @Composable () -> Unit = { EmptyState(message = stringResource(Res.string.tab_accounts_empty)) },
    envelopesAction: @Composable () -> Unit = {},
    envelopesContent: @Composable () -> Unit = { EmptyState(message = stringResource(Res.string.tab_envelopes_empty)) },
    movementsAction: @Composable () -> Unit = {
        AddIconButton(
            contentDescription = stringResource(Res.string.action_new_movement),
            onClick = { onEvent(GroupEvent.NewItemClicked) },
        )
    },
    /** Replaces the whole header on the Movements tab while it is not null (the search field). */
    movementsHeader: (@Composable () -> Unit)? = null,
    movementsContent: @Composable () -> Unit = { EmptyState(message = stringResource(Res.string.tab_movements_empty)) },
) {
    if (state.newItemSheetOpen) {
        ComingSoonSheet(
            title = stringResource(state.selectedTab.newItemLabel()),
            onDismiss = { onEvent(GroupEvent.NewItemDismissed) },
        )
    }

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
                    icon = {
                        if (tab == GroupTab.Group && state.pendingCount > 0) {
                            val badgeText = stringResource(Res.string.tab_group_badge, state.pendingCount)
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Text(state.pendingCount.toString()) }
                                },
                                modifier = Modifier.semantics { contentDescription = badgeText },
                            ) { Icon(painter = painterResource(tab.icon()), contentDescription = null) }
                        } else {
                            Icon(painter = painterResource(tab.icon()), contentDescription = null)
                        }
                    },
                    label = { Text(stringResource(tab.label())) },
                    colors = itemColors,
                )
            }
        },
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Same header on every tab. The right side is free for the actions of each section.
                if (state.selectedTab == GroupTab.Movements && movementsHeader != null) movementsHeader() else ScreenHeader(
                    title = state.group?.name.orEmpty(),
                    // A group is a root, not a page above Groups: a menu button opens them, never a back arrow
                    navigation = { MenuButton(onClick = { onEvent(GroupEvent.MenuClicked) }) },
                    leading = {
                        EntityAvatar(
                            name = state.group?.name.orEmpty(),
                            icon = iconFor(IconCategory.Group, state.group?.icon),
                            color = EntityColor.fromHex(state.group?.color),
                            size = 36.dp,
                        )
                    },
                    actions = { HeaderAction(state, onEvent, accountsAction, envelopesAction, movementsAction) },
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
                    state.selectedTab == GroupTab.Movements -> movementsContent()
                    else -> groupContent()
                }
            }
        }
    }
}

/** Right side of the header: the settings gear on the Group tab (everyone), "+" on the lists. */
@Composable
private fun HeaderAction(
    state: GroupUiState,
    onEvent: (GroupEvent) -> Unit,
    accountsAction: @Composable () -> Unit,
    envelopesAction: @Composable () -> Unit,
    movementsAction: @Composable () -> Unit,
) {
    when (state.selectedTab) {
        GroupTab.Group -> IconButton(onClick = { onEvent(GroupEvent.SettingsClicked) }) {
            Icon(
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = stringResource(Res.string.group_settings_title),
            )
        }
        GroupTab.Accounts -> accountsAction()
        GroupTab.Envelopes -> envelopesAction()
        GroupTab.Movements -> movementsAction()
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
