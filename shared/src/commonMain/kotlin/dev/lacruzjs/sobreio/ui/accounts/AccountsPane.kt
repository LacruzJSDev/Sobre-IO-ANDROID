package dev.lacruzjs.sobreio.ui.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.accounts_empty_editor
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.tab_accounts_empty
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Accounts tab: total, list, undo snackbar. It lives inside the group shell,
 * so the snackbar sits above the navigation bar. The "+" is in the shell's header.
 */
@Composable
fun AccountsPane(
    state: AccountsUiState,
    onEvent: (AccountsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The list is the only part that scrolls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.accounts.isEmpty() && state.archivedAccounts.isEmpty() && state.error != null -> EmptyState(
                    message = stringResource(state.error.messageRes()),
                ) {
                    TextButton(onClick = { onEvent(AccountsEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                state.accounts.isEmpty() && state.archivedAccounts.isEmpty() -> EmptyState(
                    message = stringResource(
                        if (state.canEdit) Res.string.accounts_empty_editor else Res.string.tab_accounts_empty,
                    ),
                )
                else -> AccountList(state, onEvent)
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage,
            onUndo = { onEvent(AccountsEvent.UndoClicked) },
            onDismiss = { onEvent(AccountsEvent.UndoExpired) },
        )
    }
}

private val previewAccounts = listOf(
    Account("1", "g", "Cuenta corriente", AccountKind.Bank, true, 129815),
    Account("2", "g", "Visa", AccountKind.CreditCard, false, -25050),
)

@PreviewLightDark
@Composable
private fun AccountsPanePreview() {
    SobreioTheme {
        AccountsPane(
            state = AccountsUiState(
                accounts = previewAccounts,
                totalCents = 104765,
                inEnvelopesCents = 129815,
                outsideCents = -25050,
                isLoading = false,
                canEdit = true,
            ),
            onEvent = {},
        )
    }
}
