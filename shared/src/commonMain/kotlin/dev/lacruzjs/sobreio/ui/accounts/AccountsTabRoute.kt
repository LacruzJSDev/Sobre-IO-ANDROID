package dev.lacruzjs.sobreio.ui.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.TabHeader
import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Accounts tab of the group shell: it creates its ViewModel (kept by the group's entry, so the tab keeps its state
 * while another is shown), draws the shell's [header] with its "+" and wires its clicks to navigation.
 */
@Composable
fun AccountsTabRoute(
    group: Group,
    header: TabHeader,
    onOpenAccountForm: (accountId: String?) -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
    onOpenTransferForm: (TransferSuggestion) -> Unit,
    viewModel: AccountsViewModel = koinViewModel { parametersOf(group.id) },
) {
    // The currency and rights reach the ViewModel before the first frame of the tab (a plain state update, safe to repeat)
    remember(viewModel, group) {
        viewModel.onEvent(AccountsEvent.GroupLoaded(group.currencyCode, group.canEdit, !group.isArchived && !group.isPendingDeletion))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Shown again each time the tab is selected, so it reads the list again
    LaunchedEffect(Unit) { viewModel.onEvent(AccountsEvent.Refresh) }
    RefreshOnRemoteChange(paused = state.undoMessage != null) { viewModel.onEvent(AccountsEvent.Refresh) }

    header(state.isRefreshing) {
        if (state.canEdit) {
            AddIconButton(contentDescription = stringResource(Res.string.action_new_account), onClick = { onOpenAccountForm(null) })
        }
    }
    AccountsPane(
        state = state,
        onEvent = { event ->
            when (event) {
                is AccountsEvent.EditClicked -> onOpenAccountForm(event.accountId)
                is AccountsEvent.OpenClicked -> onOpenAccount(event.accountId)
                is AccountsEvent.TransferClicked -> onOpenTransferForm(event.suggestion)
                else -> viewModel.onEvent(event)
            }
        },
    )
}
