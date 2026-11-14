package dev.lacruzjs.sobreio.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.accounts_total
import dev.lacruzjs.sobreio.generated.resources.action_delete
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.ic_account_balance
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.SwipeAction
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.format.formatMoney
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The total and the accounts, one row each. Owners and admins swipe a row left for Edit and Delete (one row open at a
 * time); members get plain rows. Tapping a row does nothing yet: the account detail is a later screen.
 */
@Composable
internal fun AccountList(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    var openAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openAccountId = null }

    val languageTag = Locale.current.toLanguageTag()
    val editLabel = stringResource(Res.string.action_edit)
    val deleteLabel = stringResource(Res.string.action_delete)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "total") {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.accounts_total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(state.totalCents, state.currencyCode, languageTag),
                    style = MaterialTheme.typography.headlineMedium,
                )
                ErrorText(state.actionError, modifier = Modifier.padding(top = 8.dp))
            }
        }
        items(state.accounts, key = { it.id }) { account ->
            val row: @Composable () -> Unit = { AccountRow(account, state.currencyCode, languageTag) }
            if (state.canEdit) {
                SwipeActionsRow(
                    actions = listOf(
                        SwipeAction(
                            label = editLabel,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { onEvent(AccountsEvent.EditClicked(account.id)) },
                        ),
                        SwipeAction(
                            label = deleteLabel,
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            onClick = { onEvent(AccountsEvent.DeleteClicked(account.id)) },
                        ),
                    ),
                    isOpen = openAccountId == account.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openAccountId = account.id else if (openAccountId == account.id) openAccountId = null
                    },
                    // The detail screen does not exist yet
                    onClick = {},
                    content = row,
                )
            } else {
                row()
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, currencyCode: String, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_account_balance),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(account.kind.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatMoney(account.balanceCents, currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
