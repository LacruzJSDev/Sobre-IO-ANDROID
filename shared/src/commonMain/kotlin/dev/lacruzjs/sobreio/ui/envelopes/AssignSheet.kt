package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.action_assign
import dev.lacruzjs.sobreio.generated.resources.assign_title
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_assign_from
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.MoneyAmount
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.components.unassignedOption
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet that moves money into [EnvelopesUiState.assignTarget]: an amount and the envelope it comes from, "Unassigned"
 * by default. Each source shows its balance; taking more than it has is allowed by the database, so it is not blocked.
 */
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
        var open by rememberSaveable { mutableStateOf(false) }
        EntitySelect(
            label = stringResource(Res.string.field_assign_from),
            options = state.assignSources.map { source ->
                val name = if (source.kind == EnvelopeKind.System) stringResource(Res.string.envelopes_unassigned) else source.name
                val balance = MoneyAmount(source.balanceCents, state.currencyCode)
                if (source.kind == EnvelopeKind.System) {
                    unassignedOption(name, balance = balance).copy(id = source.id)
                } else {
                    source.toEntityOption(balance = balance)
                }
            },
            selectedId = state.sourceId,
            onSelect = { id -> id?.let { onEvent(EnvelopesEvent.SourceSelected(it)) } },
            expanded = open,
            onToggle = { open = !open },
            enabled = !state.isSubmitting,
            error = state.sourceError,
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_assign),
            onClick = { onEvent(EnvelopesEvent.AssignConfirmed) },
            isLoading = state.isSubmitting,
        )
    }
}
