// The Group tab's warnings card: overspent envelopes, accounts that fall short and the transfers that cover them.

package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_uncovered
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_move
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_leaves
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_enters
import dev.lacruzjs.sobreio.generated.resources.transfer_action
import dev.lacruzjs.sobreio.domain.model.TransferHint
import dev.lacruzjs.sobreio.domain.model.BoundaryCrossing
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_summary_warnings_title
import dev.lacruzjs.sobreio.generated.resources.warning_account_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_commitments_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_envelope_overspent
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WarningsCard(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    SoftCard {
        CardTitle(stringResource(Res.string.group_summary_warnings_title))
        state.warnings.forEach { warning ->
            val text = when (warning) {
                is GroupWarning.EnvelopeOverspent -> stringResource(
                    Res.string.warning_envelope_overspent,
                    warning.name,
                    formatMoney(warning.overspentCents, state.currencyCode, languageTag),
                )
                is GroupWarning.AccountShortfallOn -> stringResource(
                    Res.string.warning_account_shortfall,
                    warning.name,
                    formatMoney(warning.lowestCents, state.currencyCode, languageTag),
                    formatDate(warning.date, languageTag),
                )
                is GroupWarning.CommitmentsShortfall -> stringResource(
                    Res.string.warning_commitments_shortfall,
                    formatMoney(warning.cents, state.currencyCode, languageTag),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEvent(GroupSummaryEvent.WarningClicked(warning)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = LocalMoneyColors.current.negative,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (warning is GroupWarning.AccountShortfallOn) {
                    warning.hints.forEach { hint -> TransferHintRow(hint, state, languageTag, onEvent) }
                }
            }
        }
    }
}

/**
 * One suggestion of `transfer_plan` under its account's warning: "Move N from X before D" (muted note when it crosses the
 * envelope boundary) and a compact "Transfer" button that opens the movement form already filled; with no source account
 * it only says that nothing can cover the amount. The button is for members who may record, in a live group.
 */
@Composable
private fun TransferHintRow(
    hint: TransferHint,
    state: GroupSummaryUiState,
    languageTag: String,
    onEvent: (GroupSummaryEvent) -> Unit,
) {
    val suggestion = hint.suggestion
    val amount = formatMoney(suggestion.amountCents, state.currencyCode, languageTag)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val fromName = hint.fromName
        if (fromName == null) {
            Text(
                text = stringResource(Res.string.transfer_hint_uncovered, amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            val move = stringResource(Res.string.transfer_hint_move, amount, fromName, formatDate(suggestion.neededBy, languageTag))
            val note = when (hint.crossing) {
                BoundaryCrossing.Enters -> stringResource(Res.string.transfer_hint_enters)
                BoundaryCrossing.Leaves -> stringResource(Res.string.transfer_hint_leaves)
                null -> null
            }
            Text(
                text = buildAnnotatedString {
                    append(move)
                    if (note != null) {
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(note) }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (state.canRecord) {
                SecondaryButton(
                    text = stringResource(Res.string.transfer_action),
                    onClick = { onEvent(GroupSummaryEvent.TransferClicked(suggestion)) },
                    compact = true,
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
}
