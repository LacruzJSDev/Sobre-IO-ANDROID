package dev.lacruzjs.sobreio.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
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
import dev.lacruzjs.sobreio.generated.resources.tab_group_badge
import dev.lacruzjs.sobreio.generated.resources.tab_movements
import dev.lacruzjs.sobreio.generated.resources.tab_movements_empty
import dev.lacruzjs.sobreio.ui.accounts.AccountsTabRoute
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ListSkeleton
import dev.lacruzjs.sobreio.ui.components.MenuButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.TabHeader
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesTabRoute
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryEvent
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryTabRoute
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryViewModel
import dev.lacruzjs.sobreio.ui.movements.MovementsTabRoute
import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import dev.lacruzjs.sobreio.ui.theme.EntityColor
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
    onCloseDetails: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenAccountForm: (accountId: String?) -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
    onOpenEnvelopeForm: (envelopeId: String?) -> Unit,
    onOpenEnvelope: (envelopeId: String) -> Unit,
    onOpenEnvelopeTransfers: () -> Unit,
    onOpenMovementForm: (movementId: String?, source: MovementSource?) -> Unit,
    onOpenTransferForm: (TransferSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel { parametersOf(groupId) },
    // The same instance the Group tab shows: the shell reads it for the tab's badge, whichever tab is open
    summaryViewModel: GroupSummaryViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summaryState by summaryViewModel.state.collectAsStateWithLifecycle()
    // Saved as the enum's position: it is the one thing the platform savers are sure to handle
    var selectedIndex by rememberSaveable { mutableStateOf(GroupTab.Group.ordinal) }
    // Every tab change goes through here: a detail open beside the list belongs to the tab being left, so it closes
    fun selectTab(tab: GroupTab) {
        if (tab.ordinal == selectedIndex) return
        selectedIndex = tab.ordinal
        onCloseDetails()
    }

    // Changes made elsewhere in the group are read as they happen (D-55): here the Group tab's figures, which give the
    // badge; each tab route reads its own while it is on display
    RefreshOnRemoteChange { summaryViewModel.onEvent(GroupSummaryEvent.Refresh) }

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
                is GroupEvent.TabSelected -> {
                    selectTab(event.tab)
                    // Shown again each time the tab is selected, so it reads the due dates again
                    if (event.tab == GroupTab.Group) summaryViewModel.onEvent(GroupSummaryEvent.Refresh)
                }
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
        tabContent = { tab, group, header ->
            when (tab) {
                GroupTab.Group -> GroupSummaryTabRoute(
                    group = group,
                    header = header,
                    onOpenSettings = onOpenSettings,
                    onOpenReview = onOpenReview,
                    onOpenAccount = onOpenAccount,
                    onOpenEnvelope = onOpenEnvelope,
                    onShowEnvelopes = { selectTab(GroupTab.Envelopes) },
                    onOpenTransferForm = onOpenTransferForm,
                    viewModel = summaryViewModel,
                )
                GroupTab.Accounts -> AccountsTabRoute(
                    group = group,
                    header = header,
                    onOpenAccountForm = onOpenAccountForm,
                    onOpenAccount = onOpenAccount,
                    onOpenTransferForm = onOpenTransferForm,
                )
                GroupTab.Envelopes -> EnvelopesTabRoute(
                    group = group,
                    header = header,
                    onOpenEnvelopeForm = onOpenEnvelopeForm,
                    onOpenEnvelope = onOpenEnvelope,
                    onOpenEnvelopeTransfers = onOpenEnvelopeTransfers,
                )
                GroupTab.Movements -> MovementsTabRoute(group = group, header = header, onOpenMovementForm = onOpenMovementForm)
            }
        },
    )
}

/**
 * The group's four sections: a bar on phones, a rail on wider windows (what NavigationSuiteScaffold picks). Each tab is
 * its own route with its own ViewModel: the shell hands it the loaded [Group] and a [TabHeader] (title, avatar and menu
 * button) that the tab draws with its own actions and refresh bar. While the group loads or fails, the shell draws the
 * header alone.
 */
@Composable
fun GroupScreen(
    state: GroupUiState,
    onEvent: (GroupEvent) -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (tab: GroupTab, group: Group, header: TabHeader) -> Unit = { tab, _, header ->
        header(false) {}
        EmptyState(message = stringResource(tab.emptyLabel()))
    },
) {
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
            Column(
                // Every tab lays out two columns on a wide pane (Movements: the filters and their totals beside the rows)
                modifier = Modifier.fillMaxSize().contentMaxWidth(ContentWidth.TwoColumns),
            ) {
                // Same header on every tab: the group, its menu button, and on the right what the tab adds
                val header: TabHeader = { tabRefreshing, actions ->
                    ScreenHeader(
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
                        actions = actions,
                        isRefreshing = state.isRefreshing || tabRefreshing,
                    )
                }
                val group = state.group
                val error = state.error
                when {
                    error != null -> {
                        header(false) {}
                        EmptyState(message = stringResource(error.messageRes())) {
                            TextButton(onClick = { onEvent(GroupEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
                        }
                    }
                    state.isLoading || group == null -> {
                        header(false) {}
                        ListSkeleton()
                    }
                    else -> tabContent(state.selectedTab, group, header)
                }
            }
        }
    }
}

/** Right side of the header: the settings gear on the Group tab (everyone), "+" on the lists. */

private fun GroupTab.icon(): DrawableResource = when (this) {
    GroupTab.Group -> Res.drawable.ic_home
    GroupTab.Accounts -> Res.drawable.ic_account_balance
    GroupTab.Envelopes -> Res.drawable.ic_envelope
    GroupTab.Movements -> Res.drawable.ic_list
}

/** What a tab says in a preview with no content. */
private fun GroupTab.emptyLabel(): StringResource = when (this) {
    GroupTab.Group -> Res.string.tab_group
    GroupTab.Accounts -> Res.string.tab_accounts_empty
    GroupTab.Envelopes -> Res.string.tab_envelopes_empty
    GroupTab.Movements -> Res.string.tab_movements_empty
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

/** A tablet-wide window: the rail shows and the Group tab would lay out in two columns. */
@Preview(widthDp = 1024, heightDp = 768)
@Composable
private fun GroupScreenTabletPreview() {
    SobreioTheme {
        GroupScreen(
            state = GroupUiState(group = Group("1", "Casa", "EUR"), isLoading = false, selectedTab = GroupTab.Accounts),
            onEvent = {},
        )
    }
}
