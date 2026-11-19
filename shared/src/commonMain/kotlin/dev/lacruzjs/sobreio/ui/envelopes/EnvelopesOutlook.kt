package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_manage_transfers
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_unknown_envelope
import dev.lacruzjs.sobreio.generated.resources.envelope_transfers_route
import dev.lacruzjs.sobreio.generated.resources.envelopes_latest_moves_title
import dev.lacruzjs.sobreio.generated.resources.envelopes_payments_empty
import dev.lacruzjs.sobreio.generated.resources.envelopes_payments_title
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.envelopetransfers.originLabel
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

/*
 * The Envelopes tab's left column on a wide pane, under the hero card: the payments the envelopes will make before the
 * end of the period and the latest moves between envelopes (read only; the swap icon opens the screen where they are
 * managed). Database answers; the app only names the envelopes.
 */

/** "Payments until <period end>": each expense due from tomorrow, the envelope it leaves and its amount. */
@Composable
internal fun PaymentsSection(state: EnvelopesUiState) {
    val periodEnd = state.periodEnd ?: return
    val languageTag = Locale.current.toLanguageTag()
    val names = envelopeNames(state)
    FormSection(title = stringResource(Res.string.envelopes_payments_title, formatDayMonth(periodEnd, languageTag))) {
        SoftTile {
            if (state.upcomingPayments.isEmpty()) {
                Text(
                    text = stringResource(Res.string.envelopes_payments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.upcomingPayments.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = listOfNotNull(formatWeekdayDayMonth(item.dueDate, languageTag), item.envelopeId?.let { names[it] })
                                .joinToString(" · "),
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

/** "Latest moves between envelopes": the newest few, read only; the swap icon opens all of them. */
@Composable
internal fun LatestMovesSection(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    if (state.latestMoves.isEmpty()) return
    val languageTag = Locale.current.toLanguageTag()
    val names = envelopeNames(state)
    val unknown = stringResource(Res.string.envelope_transfer_unknown_envelope)
    FormSection(
        title = stringResource(Res.string.envelopes_latest_moves_title),
        action = {
            IconActionButton(
                icon = Icons.Filled.SwapHoriz,
                iconSize = 24.dp,
                label = stringResource(Res.string.action_manage_transfers),
                onClick = { onEvent(EnvelopesEvent.TransfersClicked) },
            )
        },
    ) {
        SoftTile {
            state.latestMoves.forEach { move ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                Res.string.envelope_transfers_route,
                                names[move.fromEnvelopeId] ?: unknown,
                                names[move.toEnvelopeId] ?: unknown,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(Res.string.row_subtitle, move.originLabel(), formatDate(move.createdAt, languageTag)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MoneyText(
                        amountCents = move.amountCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Transfer,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Envelope names by id, "Unassigned" translated (it is stored in Spanish). */
@Composable
private fun envelopeNames(state: EnvelopesUiState): Map<String, String> {
    val unassigned = stringResource(Res.string.envelopes_unassigned)
    return (state.envelopes + state.archivedEnvelopes + listOfNotNull(state.unassigned))
        .associate { it.id to if (it.kind == EnvelopeKind.System) unassigned else it.name }
}
