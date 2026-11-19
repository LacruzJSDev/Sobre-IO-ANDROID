// Envelope detail: the read-only lists (commitments that fund it, moves between envelopes, latest movements).

package dev.lacruzjs.sobreio.ui.envelopedetail

import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_manage_transfers
import dev.lacruzjs.sobreio.generated.resources.action_see_all_movements
import dev.lacruzjs.sobreio.generated.resources.detail_no_movements
import dev.lacruzjs.sobreio.generated.resources.detail_section_latest
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_commitments
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_transfers
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_transfers_empty
import dev.lacruzjs.sobreio.generated.resources.envelope_funding_row
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.ic_list
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.transfer_from
import dev.lacruzjs.sobreio.generated.resources.transfer_to
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import dev.lacruzjs.sobreio.ui.envelopetransfers.originLabel
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** The commitments that reserve money for the envelope, read only: tapping one opens its detail. */
@Composable
internal fun FundingSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    FormSection(title = stringResource(Res.string.envelope_detail_section_commitments)) {
        state.funding.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onEvent(EnvelopeDetailEvent.CommitmentClicked(row.accountId, row.commitmentId)) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntityAvatar(name = row.name, icon = row.kind.icon(), color = null)
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(text = row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = stringResource(
                            Res.string.envelope_funding_row,
                            formatMoney(row.thisPeriodCents, state.currencyCode, languageTag),
                            formatDate(row.nextDueDate, languageTag),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * The newest moves between envelopes, read only. The icon on the right of the title opens the envelope's moves screen,
 * the only place where they are edited and deleted.
 */
@Composable
internal fun TransfersSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    FormSection(
        title = stringResource(Res.string.envelope_detail_section_transfers),
        action = {
            IconActionButton(
                icon = Icons.Filled.SwapHoriz,
                iconSize = 24.dp,
                label = stringResource(Res.string.action_manage_transfers),
                onClick = { onEvent(EnvelopeDetailEvent.TransfersClicked) },
            )
        },
    ) {
        SoftTile {
            if (state.transfers.isEmpty()) {
                Text(
                    text = stringResource(Res.string.envelope_detail_transfers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.transfers.forEach { TransferItem(it, state.currencyCode, languageTag) }
            }
        }
    }
}

@Composable
private fun TransferItem(row: TransferRow, currencyCode: String, languageTag: String) {
    val label = row.transfer.originLabel()
    val other = if (row.otherIsUnassigned) stringResource(Res.string.envelopes_unassigned) else row.otherName
    val direction = other?.let {
        stringResource(if (row.signedCents > 0) Res.string.transfer_from else Res.string.transfer_to, it)
    }
    val date = formatDate(row.date, languageTag)
    val second = if (direction != null) stringResource(Res.string.row_subtitle, direction, date) else date
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = second,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(
            amountCents = row.signedCents,
            currencyCode = currencyCode,
            role = MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
internal fun LatestMovementsSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    FormSection(
        title = stringResource(Res.string.detail_section_latest),
        action = {
            IconActionButton(
                icon = vectorResource(Res.drawable.ic_list),
                iconSize = 24.dp,
                label = stringResource(Res.string.action_see_all_movements),
                onClick = { onEvent(EnvelopeDetailEvent.SeeAllClicked) },
            )
        },
    ) {
        SoftTile {
            if (state.latestMovements.isEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_no_movements),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.latestMovements.forEach { MovementItem(it, state, languageTag) }
            }
        }
    }
}

@Composable
private fun MovementItem(movement: Movement, state: EnvelopeDetailUiState, languageTag: String) {
    val primary = movement.notes ?: stringResource(movement.kind.labelRes())
    val account = if (state.hasSeveralAccounts) state.accountNames[movement.accountId] else null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (account != null) {
                Text(
                    text = account,
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
                currencyCode = state.currencyCode,
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
