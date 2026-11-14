package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_assign
import dev.lacruzjs.sobreio.generated.resources.assign_title
import dev.lacruzjs.sobreio.generated.resources.envelope_chip_label
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_assign_from
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet that moves money into [EnvelopesUiState.assignTarget]: an amount and the envelope it comes from, "Unassigned"
 * by default. Each source shows its balance; taking more than it has is allowed by the database, so it is not blocked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AssignSheet(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    val target = state.assignTarget ?: return
    val languageTag = Locale.current.toLanguageTag()
    FlatBottomSheet(
        title = stringResource(Res.string.assign_title, target.name),
        onDismiss = { onEvent(EnvelopesEvent.SheetDismissed) },
    ) {
        AmountField(
            value = state.amount,
            onValueChange = { onEvent(EnvelopesEvent.AmountChanged(it)) },
            label = stringResource(Res.string.field_amount),
            errorRes = state.amountError,
            enabled = !state.isSubmitting,
        )
        Text(text = stringResource(Res.string.field_assign_from), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.assignSources.forEach { source ->
                val name = if (source.kind == EnvelopeKind.System) {
                    stringResource(source.kind.labelRes())
                } else {
                    source.name
                }
                FilterChip(
                    selected = state.sourceId == source.id,
                    onClick = { onEvent(EnvelopesEvent.SourceSelected(source.id)) },
                    enabled = !state.isSubmitting,
                    label = {
                        Text(
                            stringResource(
                                Res.string.envelope_chip_label,
                                name,
                                formatMoney(source.balanceCents, state.currencyCode, languageTag),
                            ),
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = null,
                )
            }
        }
        state.sourceError?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_assign),
            onClick = { onEvent(EnvelopesEvent.AssignConfirmed) },
            isLoading = state.isSubmitting,
        )
    }
}
