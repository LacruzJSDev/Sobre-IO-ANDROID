package dev.lacruzjs.sobreio.ui.envelopetransfers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_automatic_note
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_edit_title
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_new_title
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_from
import dev.lacruzjs.sobreio.generated.resources.field_notes
import dev.lacruzjs.sobreio.generated.resources.field_to
import dev.lacruzjs.sobreio.generated.resources.form_section_envelope_transfer
import dev.lacruzjs.sobreio.ui.components.FormSkeleton
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.MoneyAmount
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.components.unassignedOption
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EnvelopeTransferFormRoute(
    groupId: String,
    transferId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** A new move opened from one envelope's moves starts with that envelope as the destination. */
    toEnvelopeId: String? = null,
    viewModel: EnvelopeTransferFormViewModel = koinViewModel { parametersOf(groupId, transferId, toEnvelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.saved) {
        LaunchedEffect(Unit) {
            onClose()
            viewModel.onEvent(EnvelopeTransferFormEvent.NavigationHandled)
        }
    }
    EnvelopeTransferFormScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                EnvelopeTransferFormEvent.CloseClicked -> onClose()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * The full-screen form of a move between envelopes: the amount, where it comes from and where it goes (each envelope
 * with its balance) and notes. An automatic move (a monthly target) shows its envelopes read only, with a note.
 */
@Composable
fun EnvelopeTransferFormScreen(
    state: EnvelopeTransferFormUiState,
    onEvent: (EnvelopeTransferFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScaffold(
        title = stringResource(if (state.isEditing) Res.string.envelope_transfer_edit_title else Res.string.envelope_transfer_new_title),
        onClose = { onEvent(EnvelopeTransferFormEvent.CloseClicked) },
        onSave = { onEvent(EnvelopeTransferFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> FormSkeleton()
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(EnvelopeTransferFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                var open by rememberSaveable { mutableStateOf(OpenTile.None) }
                fun toggle(tile: OpenTile) { open = if (open == tile) OpenTile.None else tile }
                val unassignedName = stringResource(Res.string.envelopes_unassigned)
                val options = state.envelopes.map { envelope ->
                    val balance = MoneyAmount(envelope.balanceCents, state.currencyCode)
                    if (envelope.kind == EnvelopeKind.System) {
                        unassignedOption(unassignedName, balance = balance).copy(id = envelope.id)
                    } else {
                        envelope.toEntityOption(balance = balance)
                    }
                }
                val editable = !state.isSubmitting

                AmountField(
                    value = state.amount,
                    onValueChange = { onEvent(EnvelopeTransferFormEvent.AmountChanged(it)) },
                    label = stringResource(Res.string.field_amount),
                    errorRes = state.amountError,
                    enabled = editable,
                )
                FormSection(title = stringResource(Res.string.form_section_envelope_transfer)) {
                    EntitySelect(
                        label = stringResource(Res.string.field_from),
                        options = options,
                        selectedId = state.fromId,
                        onSelect = { id -> id?.let { onEvent(EnvelopeTransferFormEvent.FromSelected(it)) } },
                        expanded = open == OpenTile.From,
                        onToggle = { toggle(OpenTile.From) },
                        enabled = editable && !state.envelopesLocked,
                        error = state.fromError,
                    )
                    EntitySelect(
                        label = stringResource(Res.string.field_to),
                        options = options.filter { it.id != state.fromId || state.envelopesLocked },
                        selectedId = state.toId,
                        onSelect = { id -> id?.let { onEvent(EnvelopeTransferFormEvent.ToSelected(it)) } },
                        expanded = open == OpenTile.To,
                        onToggle = { toggle(OpenTile.To) },
                        enabled = editable && !state.envelopesLocked,
                        error = state.toError,
                    )
                    if (state.envelopesLocked) InlineNotice(stringResource(Res.string.envelope_transfer_automatic_note))
                }
                FormTextField(
                    value = state.notes,
                    onValueChange = { onEvent(EnvelopeTransferFormEvent.NotesChanged(it)) },
                    label = stringResource(Res.string.field_notes),
                    capitalization = KeyboardCapitalization.Sentences,
                    enabled = editable,
                    imeAction = ImeAction.Done,
                )
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Which envelope choice is open; one at a time keeps the form short. */
private enum class OpenTile { None, From, To }

@PreviewLightDark
@Composable
private fun EnvelopeTransferFormScreenPreview() {
    SobreioTheme { EnvelopeTransferFormScreen(state = EnvelopeTransferFormUiState(isLoading = false), onEvent = {}) }
}
