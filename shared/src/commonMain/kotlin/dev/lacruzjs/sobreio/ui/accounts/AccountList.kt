package dev.lacruzjs.sobreio.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.accounts_in_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.accounts_outside_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.accounts_total
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/**
 * The summary, the accounts in two sections (in the envelopes and outside them) and, at the end, a collapsed
 * "Archived" section. Owners and admins swipe a live row left for Edit and Delete and an archived row for Reopen (one
 * row open at a time); members get plain rows. Tapping a row does nothing yet: the account detail is a later screen.
 */
@Composable
internal fun AccountList(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openAccountId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openAccountId = null }

    val languageTag = Locale.current.toLanguageTag()
    val outside = state.outsideEnvelopes

    val rows: LazyListScope.(List<Account>, Boolean) -> Unit = { list, archived ->
        items(list, key = { (if (archived) "archived-" else "") + it.id }) { account ->
            val row: @Composable () -> Unit = { AccountRow(account, state, languageTag) }
            if (state.canEdit) {
                SwipeActionsRow(
                    actions = if (archived) {
                        listOf(reopenAction { onEvent(AccountsEvent.ReopenClicked(account.id)) })
                    } else {
                        listOf(
                            editAction { onEvent(AccountsEvent.EditClicked(account.id)) },
                            deleteAction { onEvent(AccountsEvent.DeleteClicked(account.id)) },
                        )
                    },
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

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "summary") { AccountsSummaryBlock(state, languageTag) }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        // The "In envelopes" title only helps when there is a second section to tell it from
        if (outside.isNotEmpty() && state.inEnvelopes.isNotEmpty()) {
            item(key = "title-in") { SectionTitle(stringResource(Res.string.accounts_in_envelopes_title)) }
        }
        rows(state.inEnvelopes, false)
        if (outside.isNotEmpty()) {
            item(key = "title-outside") { SectionTitle(stringResource(Res.string.accounts_outside_envelopes_title)) }
            rows(outside, false)
        }
        if (state.archivedAccounts.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedAccounts.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            if (archivedExpanded) rows(state.archivedAccounts, true)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** The total and, when some accounts are outside the envelopes, how it splits between inside and outside. */
@Composable
private fun AccountsSummaryBlock(state: AccountsUiState, languageTag: String) {
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
        if (state.outsideEnvelopes.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                SmallFigure(stringResource(Res.string.accounts_in_envelopes_title), formatMoney(state.inEnvelopesCents, state.currencyCode, languageTag))
                SmallFigure(stringResource(Res.string.accounts_outside_envelopes_title), formatMoney(state.outsideCents, state.currencyCode, languageTag))
            }
        }
    }
}

@Composable
private fun SmallFigure(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AccountRow(account: Account, state: AccountsUiState, languageTag: String) {
    val kind = stringResource(account.kind.labelRes())
    val subtitle = if (state.hasSeveralMembers) {
        val holder = state.holderName(account) ?: stringResource(Res.string.holder_joint)
        stringResource(Res.string.row_subtitle, kind, holder)
    } else {
        kind
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(
            name = account.name,
            icon = iconFor(IconCategory.Account, account.icon),
            color = EntityColor.fromHex(account.color),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatMoney(account.balanceCents, state.currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
