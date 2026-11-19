// Account detail Summary: next due items and the latest movements.

package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.ui.components.SoftTile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_see_all_movements
import dev.lacruzjs.sobreio.generated.resources.ic_list
import dev.lacruzjs.sobreio.generated.resources.detail_no_movements
import dev.lacruzjs.sobreio.generated.resources.detail_section_latest
import dev.lacruzjs.sobreio.generated.resources.detail_section_upcoming
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.movement_from_account
import dev.lacruzjs.sobreio.generated.resources.movement_to_account
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.MoneyText
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import dev.lacruzjs.sobreio.ui.components.IconActionButton

@Composable
internal fun UpcomingSection(state: AccountDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.detail_section_upcoming)) {
        SoftTile {
            state.upcoming.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = formatDate(item.dueDate, languageTag),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MoneyText(
                        amountCents = item.amountCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Flow,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LatestMovementsSection(state: AccountDetailUiState, languageTag: String, onEvent: (AccountDetailEvent) -> Unit) {
    FormSection(
        title = stringResource(Res.string.detail_section_latest),
        action = {
            IconActionButton(
                icon = vectorResource(Res.drawable.ic_list),
                iconSize = 24.dp,
                label = stringResource(Res.string.action_see_all_movements),
                onClick = { onEvent(AccountDetailEvent.SeeAllClicked) },
            )
        },
    ) {
        SoftTile {
            if (state.movements.isEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_no_movements),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.movements.forEach { MovementRow(it, state.currencyCode, languageTag) }
            }
        }
    }
}

@Composable
private fun MovementRow(row: MovementRowState, currencyCode: String, languageTag: String) {
    val movement = row.movement
    val primary = movement.notes ?: stringResource(movement.kind.labelRes())
    val secondary = when {
        row.counterAccountName != null && movement.kind == MovementKind.TransferOut ->
            stringResource(Res.string.movement_to_account, row.counterAccountName)
        row.counterAccountName != null && movement.kind == MovementKind.TransferIn ->
            stringResource(Res.string.movement_from_account, row.counterAccountName)
        row.envelopeIsUnassigned -> stringResource(Res.string.envelopes_unassigned)
        else -> row.envelopeName
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EntityAvatar(name = row.authorName.orEmpty(), icon = null, color = null, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                amountCents = movement.amountCents,
                currencyCode = currencyCode,
                role = MoneyRole.Flow,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                text = formatDayMonth(movement.date, languageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
