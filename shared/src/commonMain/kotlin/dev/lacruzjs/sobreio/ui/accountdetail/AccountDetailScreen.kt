package dev.lacruzjs.sobreio.ui.accountdetail

import androidx.compose.foundation.layout.fillMaxHeight
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_detail_tab_commitments
import dev.lacruzjs.sobreio.generated.resources.account_detail_tab_summary
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.action_new_commitment
import dev.lacruzjs.sobreio.generated.resources.edit_account_title
import dev.lacruzjs.sobreio.ui.commitments.AccountCommitmentsViewModel
import dev.lacruzjs.sobreio.ui.commitments.CommitmentsEvent
import dev.lacruzjs.sobreio.ui.commitments.CommitmentsPane
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.DetailSkeleton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatSignedMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import dev.lacruzjs.sobreio.ui.undo.CommitUndoOnBack
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 2 of the Accounts tab. Holds the lambdas of the back stack: [onEdit] opens the account form, [onSeeAllMovements]
 * pushes the account's movements, [onImport] the bank import (see AppNavigation).
 */
@Composable
fun AccountDetailRoute(
    groupId: String,
    accountId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSeeAllMovements: () -> Unit,
    onImport: () -> Unit,
    onOpenCommitmentForm: (commitmentId: String?) -> Unit,
    onOpenCommitment: (commitmentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountDetailViewModel = koinViewModel { parametersOf(groupId, accountId) },
    commitmentsViewModel: AccountCommitmentsViewModel = koinViewModel { parametersOf(groupId, accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val commitmentsState by commitmentsViewModel.state.collectAsStateWithLifecycle()
    // Saved as the enum's position, like the group's tabs
    var tabIndex by rememberSaveable { mutableStateOf(AccountDetailTab.Summary.ordinal) }

    // Shown again after the edit form: the name, colour or figures may have changed
    LaunchedEffect(Unit) { viewModel.onEvent(AccountDetailEvent.Refresh) }
    val leave = {
        viewModel.onEvent(AccountDetailEvent.BackClicked)
        onBack()
    }
    CommitUndoOnBack(pending = state.undoMessage != null, onLeave = leave)

    AccountDetailScreen(
        // The thin bar under the header covers the Commitments tab's reads too
        state = state.copy(isRefreshing = state.isRefreshing || commitmentsState.isRefreshing),
        selectedTab = AccountDetailTab.entries[tabIndex],
        onEvent = { event ->
            when (event) {
                AccountDetailEvent.BackClicked -> leave()
                AccountDetailEvent.EditClicked -> onEdit()
                AccountDetailEvent.SeeAllClicked -> onSeeAllMovements()
                AccountDetailEvent.BankImportClicked -> onImport()
                is AccountDetailEvent.TabSelected -> tabIndex = event.tab.ordinal
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
        commitmentsAction = {
            if (commitmentsState.canEdit) {
                AddIconButton(
                    contentDescription = stringResource(Res.string.action_new_commitment),
                    onClick = { onOpenCommitmentForm(null) },
                )
            }
        },
        commitmentsContent = {
            // Shown again each time the tab is selected or the form closes, so it reads the list again
            LaunchedEffect(Unit) { commitmentsViewModel.onEvent(CommitmentsEvent.Refresh) }
            CommitmentsPane(
                state = commitmentsState,
                onEvent = { event ->
                    when (event) {
                        is CommitmentsEvent.EditClicked -> onOpenCommitmentForm(event.commitmentId)
                        is CommitmentsEvent.OpenClicked -> onOpenCommitment(event.commitmentId)
                        else -> commitmentsViewModel.onEvent(event)
                    }
                },
            )
        },
    )
}

@Composable
fun AccountDetailScreen(
    state: AccountDetailUiState,
    selectedTab: AccountDetailTab,
    onEvent: (AccountDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
    /** The header "+" of the Commitments tab and the tab's content, filled by the Route from its own ViewModel. */
    commitmentsAction: @Composable () -> Unit = {},
    commitmentsContent: @Composable () -> Unit = {},
) {
    val languageTag = Locale.current.toLanguageTag()
    val account = state.account
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                isRefreshing = state.isRefreshing,
                title = account?.name.orEmpty(),
                subtitle = state.groupName.takeIf { it.isNotEmpty() },
                onBack = { onEvent(AccountDetailEvent.BackClicked) },
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
                    if (selectedTab == AccountDetailTab.Commitments) {
                        // The pencil edits the account and stays on the Summary tab
                        if (account != null) commitmentsAction()
                    } else if (account != null && state.canEdit) {
                        IconActionButton(
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.edit_account_title),
                            onClick = { onEvent(AccountDetailEvent.EditClicked) },
                        )
                    }
                },
            )
            Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                when {
                    state.isLoading -> DetailSkeleton()
                    state.error != null || account == null -> EmptyState(
                        message = stringResource((state.error ?: AppError.NotFound).messageRes()),
                    ) {
                        TextButton(onClick = { onEvent(AccountDetailEvent.Retry) }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                    else -> {
                        DetailTabs(selectedTab, onEvent)
                        Box(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                            when (selectedTab) {
                                AccountDetailTab.Summary -> AccountSummaryTab(state, onEvent)
                                AccountDetailTab.Commitments -> Box(Modifier.fillMaxHeight().contentMaxWidth()) { commitmentsContent() }
                            }
                        }
                    }
                }
                // The adjustment's amount is written here, where the language and the currency are known
                val undoMessage = state.undoMessage?.let { message ->
                    message.amountCents
                        ?.let { cents -> message.copy(argument = formatSignedMoney(cents, state.currencyCode, languageTag)) }
                        ?: message
                }
                UndoSnackbarHost(
                    message = undoMessage,
                    onUndo = { onEvent(AccountDetailEvent.UndoClicked) },
                    onDismiss = { onEvent(AccountDetailEvent.UndoExpired) },
                )
            }
        }
        state.compare?.let { CompareSheet(it, state.account?.balanceCents ?: 0L, state.currencyCode, onEvent) }
    }
}

/** Flat top tabs: paper background, ink indicator, no divider. */
@Composable
private fun DetailTabs(selected: AccountDetailTab, onEvent: (AccountDetailEvent) -> Unit) {
    PrimaryTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
    ) {
        AccountDetailTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onEvent(AccountDetailEvent.TabSelected(tab)) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        stringResource(
                            when (tab) {
                                AccountDetailTab.Summary -> Res.string.account_detail_tab_summary
                                AccountDetailTab.Commitments -> Res.string.account_detail_tab_commitments
                            },
                        ),
                    )
                },
            )
        }
    }
}

private val previewAccount = Account("a", "g", "Cuenta corriente", AccountKind.Bank, true, 129_815, color = "#4F7A5A", icon = "bank")

private val previewState = AccountDetailUiState(
    isLoading = false,
    groupName = "Casa",
    canEdit = true,
    account = previewAccount,
    today = LocalDate(2026, 11, 14),
    periodEnd = LocalDate(2026, 11, 30),
    balanceSeries = listOf(
        BalancePoint(LocalDate(2026, 8, 14), 90_000),
        BalancePoint(LocalDate(2026, 9, 14), 120_000),
        BalancePoint(LocalDate(2026, 10, 14), 80_000),
        BalancePoint(LocalDate(2026, 11, 14), 129_815),
    ),
    projection = listOf(
        ProjectionPoint(LocalDate(2026, 11, 14), 129_815, 0),
        ProjectionPoint(LocalDate(2026, 11, 20), 60_000, -69_815),
        ProjectionPoint(LocalDate(2026, 11, 28), 160_000, 100_000),
    ),
    forecast = AccountForecast(129_815, 100_000, 69_815, 160_000, 60_000, 12_000),
    shortfallDate = LocalDate(2026, 11, 20),
    flows = listOf(
        PeriodFlow("1", LocalDate(2026, 6, 1), LocalDate(2026, 6, 30), 200_000, 180_000),
        PeriodFlow("2", LocalDate(2026, 7, 1), LocalDate(2026, 7, 31), 210_000, 250_000),
        PeriodFlow("3", LocalDate(2026, 8, 1), LocalDate(2026, 8, 31), 190_000, 160_000),
        PeriodFlow("4", LocalDate(2026, 9, 1), LocalDate(2026, 9, 30), 230_000, 170_000),
        PeriodFlow("5", LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), 205_000, 190_000),
        PeriodFlow("6", LocalDate(2026, 11, 1), LocalDate(2026, 11, 30), 120_000, 90_000),
    ),
    upcoming = listOf(UpcomingItem("c", "Alquiler", LocalDate(2026, 11, 20), -69_815)),
    movements = listOf(
        MovementRowState(
            AccountMovement("m", MovementKind.Expense, -4_250, LocalDate(2026, 11, 13), "Supermercado", null, "e", "u"),
            authorName = "Ana",
            envelopeName = "Comida",
            envelopeIsUnassigned = false,
            counterAccountName = null,
        ),
    ),
)

@PreviewLightDark
@Composable
private fun AccountDetailScreenPreview() {
    SobreioTheme {
        AccountDetailScreen(state = previewState, selectedTab = AccountDetailTab.Summary, onEvent = {})
    }
}

/** A tablet-wide pane: the summary lays out in two columns. */
@Preview(widthDp = 1024, heightDp = 768)
@Composable
private fun AccountDetailScreenTabletPreview() {
    SobreioTheme {
        AccountDetailScreen(state = previewState, selectedTab = AccountDetailTab.Summary, onEvent = {})
    }
}
