package dev.lacruzjs.sobreio.ui.accounts

import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.accounts_plan_move
import dev.lacruzjs.sobreio.generated.resources.accounts_plan_title
import dev.lacruzjs.sobreio.generated.resources.accounts_plan_uncovered
import dev.lacruzjs.sobreio.generated.resources.accounts_upcoming_empty
import dev.lacruzjs.sobreio.generated.resources.accounts_upcoming_title
import dev.lacruzjs.sobreio.generated.resources.transfer_action
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

/*
 * The Accounts tab's left column on a wide pane, under the hero card: what the accounts need before the end of the
 * period. Both blocks are database answers (`transfer_plan`, `upcoming_occurrences`); the app only names the accounts.
 */

/** "To keep every account above zero": each move `transfer_plan` suggests, with "Transfer" for members who may record. */
@Composable
internal fun PlanSection(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    if (state.plan.isEmpty()) return
    val languageTag = Locale.current.toLanguageTag()
    val names = (state.accounts + state.archivedAccounts).associate { it.id to it.name }
    FormSection(title = stringResource(Res.string.accounts_plan_title)) {
        SoftTile {
            state.plan.forEach { suggestion ->
                val amount = formatMoney(suggestion.amountCents, state.currencyCode, languageTag)
                val to = names[suggestion.toAccountId].orEmpty()
                val from = suggestion.fromAccountId?.let { names[it] }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (from == null) {
                            stringResource(Res.string.accounts_plan_uncovered, amount, to)
                        } else {
                            stringResource(Res.string.accounts_plan_move, amount, from, to, formatDate(suggestion.neededBy, languageTag))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (from != null && state.canRecord) {
                        SecondaryButton(
                            text = stringResource(Res.string.transfer_action),
                            onClick = { onEvent(AccountsEvent.TransferClicked(suggestion)) },
                            compact = true,
                            modifier = Modifier.width(112.dp),
                        )
                    }
                }
            }
        }
    }
}

/** "Coming up until <period end>": every due date of the accounts from tomorrow, its account and its signed amount. */
@Composable
internal fun AccountsUpcomingSection(state: AccountsUiState) {
    val periodEnd = state.periodEnd ?: return
    val languageTag = Locale.current.toLanguageTag()
    val names = (state.accounts + state.archivedAccounts).associate { it.id to it.name }
    FormSection(title = stringResource(Res.string.accounts_upcoming_title, formatDayMonth(periodEnd, languageTag))) {
        SoftTile {
            if (state.upcoming.isEmpty()) {
                Text(
                    text = stringResource(Res.string.accounts_upcoming_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.upcoming.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = listOfNotNull(
                                formatWeekdayDayMonth(item.dueDate, languageTag),
                                item.accountId?.let { names[it] },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MoneyText(
                        amountCents = item.signedCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Flow,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
