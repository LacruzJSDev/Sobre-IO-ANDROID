package dev.lacruzjs.sobreio.ui.commitmentform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.commitment_account_change_note
import dev.lacruzjs.sobreio.generated.resources.commitment_account_linked_note
import dev.lacruzjs.sobreio.generated.resources.commitment_effective_note
import dev.lacruzjs.sobreio.generated.resources.commitment_funded_note
import dev.lacruzjs.sobreio.generated.resources.commitment_linked_note
import dev.lacruzjs.sobreio.generated.resources.commitment_no_groups_note
import dev.lacruzjs.sobreio.generated.resources.commitment_outside_note
import dev.lacruzjs.sobreio.generated.resources.commitment_register_note
import dev.lacruzjs.sobreio.generated.resources.edit_commitment_title
import dev.lacruzjs.sobreio.generated.resources.action_new_commitment
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.every_next
import dev.lacruzjs.sobreio.generated.resources.every_previous
import dev.lacruzjs.sobreio.generated.resources.every_value
import dev.lacruzjs.sobreio.generated.resources.field_account
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_auto_register
import dev.lacruzjs.sobreio.generated.resources.field_commitment_kind
import dev.lacruzjs.sobreio.generated.resources.field_commitment_name
import dev.lacruzjs.sobreio.generated.resources.field_dest_account
import dev.lacruzjs.sobreio.generated.resources.field_dest_envelope
import dev.lacruzjs.sobreio.generated.resources.field_dest_group
import dev.lacruzjs.sobreio.generated.resources.field_effective_from
import dev.lacruzjs.sobreio.generated.resources.field_end_date
import dev.lacruzjs.sobreio.generated.resources.field_envelope
import dev.lacruzjs.sobreio.generated.resources.field_every
import dev.lacruzjs.sobreio.generated.resources.field_first_due
import dev.lacruzjs.sobreio.generated.resources.field_funded
import dev.lacruzjs.sobreio.generated.resources.field_notes
import dev.lacruzjs.sobreio.generated.resources.field_repeats
import dev.lacruzjs.sobreio.generated.resources.field_source_envelope
import dev.lacruzjs.sobreio.generated.resources.field_to_account
import dev.lacruzjs.sobreio.generated.resources.field_unit
import dev.lacruzjs.sobreio.generated.resources.form_section_calendar
import dev.lacruzjs.sobreio.generated.resources.form_section_commitment
import dev.lacruzjs.sobreio.generated.resources.form_section_effective
import dev.lacruzjs.sobreio.generated.resources.form_section_money
import dev.lacruzjs.sobreio.generated.resources.form_section_notes
import dev.lacruzjs.sobreio.generated.resources.form_section_registration
import dev.lacruzjs.sobreio.generated.resources.end_date_never
import dev.lacruzjs.sobreio.generated.resources.unit_days
import dev.lacruzjs.sobreio.generated.resources.unit_months
import dev.lacruzjs.sobreio.generated.resources.unit_weeks
import dev.lacruzjs.sobreio.generated.resources.unit_years
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.commitments.labelRes
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ExpandableFormTile
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.expandedStateText
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CommitmentFormRoute(
    groupId: String,
    accountId: String,
    commitmentId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommitmentFormViewModel = koinViewModel { parametersOf(groupId, accountId, commitmentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.saved) {
        LaunchedEffect(Unit) {
            onClose()
            viewModel.onEvent(CommitmentFormEvent.NavigationHandled)
        }
    }
    CommitmentFormScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                CommitmentFormEvent.CloseClicked -> onClose()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/** Which expandable tile of the form is open; one at a time keeps the form short. */
private enum class OpenTile { None, Kind, Account, ToAccount, Envelope, DestGroup, DestAccount, DestEnvelope, Unit }

/**
 * Same layout as the other forms: the kind's icon next to the name, then sections of tiles: Commitment (type, amount),
 * Money, Calendar, Registration, Notes and, when editing, "Apply from".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommitmentFormScreen(
    state: CommitmentFormUiState,
    onEvent: (CommitmentFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScaffold(
        title = stringResource(if (state.isEditing) Res.string.edit_commitment_title else Res.string.action_new_commitment),
        onClose = { onEvent(CommitmentFormEvent.CloseClicked) },
        onSave = { onEvent(CommitmentFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(CommitmentFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                var open by rememberSaveable { mutableStateOf(OpenTile.None) }
                fun toggle(tile: OpenTile) { open = if (open == tile) OpenTile.None else tile }
                val languageTag = Locale.current.toLanguageTag()
                val enabled = !state.isSubmitting
                val unassigned = stringResource(Res.string.envelopes_unassigned)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EntityAvatar(name = state.name, icon = state.kind.icon(), color = null, size = 64.dp)
                    FormTextField(
                        value = state.name,
                        onValueChange = { onEvent(CommitmentFormEvent.NameChanged(it)) },
                        label = stringResource(Res.string.field_commitment_name),
                        errorRes = state.nameError,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_commitment)) {
                    if (state.isEditing) {
                        FormTile(label = stringResource(Res.string.field_commitment_kind)) {
                            FormTileValue(stringResource(state.kind.labelRes()))
                        }
                    } else {
                        ExpandableFormTile(
                            label = stringResource(Res.string.field_commitment_kind),
                            expanded = open == OpenTile.Kind,
                            onToggle = { toggle(OpenTile.Kind) },
                            expandedDescription = expandedStateText(open == OpenTile.Kind),
                            preview = { FormTileValue(stringResource(state.kind.labelRes())) },
                        ) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CreatableKinds.forEach { kind ->
                                    FormChip(
                                        label = stringResource(kind.labelRes()),
                                        selected = state.kind == kind,
                                        enabled = enabled,
                                        onClick = { onEvent(CommitmentFormEvent.KindSelected(kind)) },
                                    )
                                }
                            }
                        }
                    }
                    AmountField(
                        value = state.amount,
                        onValueChange = { onEvent(CommitmentFormEvent.AmountChanged(it)) },
                        label = stringResource(Res.string.field_amount),
                        errorRes = state.amountError,
                        enabled = enabled,
                    )
                    if (state.fieldsLocked) FormNote(stringResource(Res.string.commitment_linked_note))
                }

                FormSection(title = stringResource(Res.string.form_section_money)) {
                    ChoiceTile(
                        label = stringResource(Res.string.field_account),
                        tile = OpenTile.Account,
                        open = open,
                        onToggle = ::toggle,
                        preview = state.accountName,
                        // Creating keeps the account of the detail; a contribution's account belongs to both groups
                        locked = !state.canChangeAccount,
                        error = null,
                    ) {
                        state.accounts.forEach { a ->
                            FormChip(a.name, state.accountId == a.id, enabled) {
                                onEvent(CommitmentFormEvent.AccountSelected(a.id))
                            }
                        }
                    }
                    if (state.isLinkedEdit) {
                        FormNote(stringResource(Res.string.commitment_account_linked_note))
                    } else if (state.accountMoved) {
                        val oldName = state.accounts.firstOrNull { it.id == state.storedAccountId }?.name.orEmpty()
                        FormNote(
                            stringResource(
                                Res.string.commitment_account_change_note,
                                state.effectiveFrom?.let { formatDate(it, languageTag) }.orEmpty(),
                                state.accountName,
                                oldName,
                            ),
                        )
                    }
                    if (state.kind == CommitmentKind.Transfer) {
                        ChoiceTile(
                            label = stringResource(Res.string.field_to_account),
                            tile = OpenTile.ToAccount,
                            open = open,
                            onToggle = ::toggle,
                            preview = state.accounts.firstOrNull { it.id == state.toAccountId }?.name.orEmpty(),
                            locked = state.fieldsLocked,
                            error = state.toAccountError,
                        ) {
                            state.destinationAccounts.forEach { a ->
                                FormChip(a.name, state.toAccountId == a.id, enabled) {
                                    onEvent(CommitmentFormEvent.ToAccountSelected(a.id))
                                }
                            }
                        }
                    }
                    if (state.kind == CommitmentKind.Expense) {
                        if (state.showsExpenseEnvelope) {
                            ChoiceTile(
                                label = stringResource(Res.string.field_envelope),
                                tile = OpenTile.Envelope,
                                open = open,
                                onToggle = ::toggle,
                                preview = state.envelopes.firstOrNull { it.id == state.envelopeId }?.name.orEmpty(),
                                locked = state.fieldsLocked,
                                error = state.envelopeError,
                            ) {
                                state.envelopes.forEach { e ->
                                    FormChip(e.name, state.envelopeId == e.id, enabled) {
                                        onEvent(CommitmentFormEvent.EnvelopeSelected(e.id))
                                    }
                                }
                            }
                        } else {
                            FormNote(stringResource(Res.string.commitment_outside_note))
                        }
                    }
                    if (state.isContribution) {
                        ContributionTiles(state, open, ::toggle, onEvent, unassigned)
                    }
                }

                FormSection(title = stringResource(Res.string.form_section_calendar)) {
                    DateFormTile(
                        label = stringResource(Res.string.field_first_due),
                        date = state.firstDueDate,
                        onDateSelected = { onEvent(CommitmentFormEvent.FirstDueDateChanged(it)) },
                        enabled = enabled && !state.fieldsLocked,
                    )
                    FormTile(label = stringResource(Res.string.field_repeats)) {
                        Switch(
                            checked = state.isRecurring,
                            onCheckedChange = { onEvent(CommitmentFormEvent.RecurringChanged(it)) },
                            enabled = enabled && !state.fieldsLocked,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (state.isRecurring) {
                        FormTile(label = stringResource(Res.string.field_every)) {
                            EveryStepper(
                                value = state.everyN,
                                enabled = enabled && !state.fieldsLocked,
                                onChange = { onEvent(CommitmentFormEvent.EveryChanged(it)) },
                            )
                        }
                        ExpandableFormTile(
                            label = stringResource(Res.string.field_unit),
                            expanded = open == OpenTile.Unit,
                            onToggle = { toggle(OpenTile.Unit) },
                            expandedDescription = expandedStateText(open == OpenTile.Unit),
                            preview = { FormTileValue(stringResource(state.unit.labelRes())) },
                        ) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FrequencyUnit.entries.forEach { unit ->
                                    FormChip(
                                        label = stringResource(unit.labelRes()),
                                        selected = state.unit == unit,
                                        enabled = enabled && !state.fieldsLocked,
                                        onClick = { onEvent(CommitmentFormEvent.UnitSelected(unit)) },
                                    )
                                }
                            }
                        }
                        DateFormTile(
                            label = stringResource(Res.string.field_end_date),
                            date = state.endDate,
                            emptyText = stringResource(Res.string.end_date_never),
                            onDateSelected = { onEvent(CommitmentFormEvent.EndDateChanged(it)) },
                            enabled = enabled && !state.fieldsLocked,
                            onClear = { onEvent(CommitmentFormEvent.EndDateChanged(null)) },
                        )
                        state.endDateError?.let { error ->
                            Text(
                                text = stringResource(error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }

                FormSection(title = stringResource(Res.string.form_section_registration)) {
                    FormTile(label = stringResource(Res.string.field_auto_register), note = stringResource(Res.string.commitment_register_note)) {
                        Switch(
                            checked = state.autoRegister,
                            onCheckedChange = { onEvent(CommitmentFormEvent.AutoRegisterChanged(it)) },
                            enabled = enabled && !state.fieldsLocked,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (state.canFund) {
                        FormTile(label = stringResource(Res.string.field_funded), note = stringResource(Res.string.commitment_funded_note)) {
                            Switch(
                                checked = state.funded,
                                onCheckedChange = { onEvent(CommitmentFormEvent.FundedChanged(it)) },
                                enabled = enabled && !state.fieldsLocked,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }

                if (!state.isContribution) {
                    FormSection(title = stringResource(Res.string.form_section_notes)) {
                        FormTextField(
                            value = state.notes,
                            onValueChange = { onEvent(CommitmentFormEvent.NotesChanged(it)) },
                            label = stringResource(Res.string.field_notes),
                            enabled = enabled,
                            imeAction = ImeAction.Done,
                        )
                    }
                }

                if (state.isEditing) {
                    FormSection(title = stringResource(Res.string.form_section_effective)) {
                        DateFormTile(
                            label = stringResource(Res.string.field_effective_from),
                            date = state.effectiveFrom,
                            onDateSelected = { onEvent(CommitmentFormEvent.EffectiveFromChanged(it)) },
                            note = stringResource(Res.string.commitment_effective_note),
                            enabled = enabled,
                        )
                    }
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private val CreatableKinds = listOf(
    CommitmentKind.Expense,
    CommitmentKind.Income,
    CommitmentKind.Transfer,
    CommitmentKind.GroupTransferOut,
)

/** An expandable tile of chips that becomes a plain read-only tile when [locked]; [error] shows under it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceTile(
    label: String,
    tile: OpenTile,
    open: OpenTile,
    onToggle: (OpenTile) -> Unit,
    preview: String,
    locked: Boolean,
    error: org.jetbrains.compose.resources.StringResource?,
    chips: @Composable () -> Unit,
) {
    if (locked) {
        FormTile(label = label) { FormTileValue(preview) }
    } else {
        ExpandableFormTile(
            label = label,
            expanded = open == tile,
            onToggle = { onToggle(tile) },
            expandedDescription = expandedStateText(open == tile),
            preview = { FormTileValue(preview) },
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        }
    }
    error?.let {
        Text(
            text = stringResource(it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** Destination group, its account, and the optional envelopes on both sides of a contribution. */
@Composable
private fun ContributionTiles(
    state: CommitmentFormUiState,
    open: OpenTile,
    onToggle: (OpenTile) -> Unit,
    onEvent: (CommitmentFormEvent) -> Unit,
    unassigned: String,
) {
    val enabled = !state.isSubmitting
    val creating = !state.isEditing
    if (creating && state.groups.isEmpty()) FormNote(stringResource(Res.string.commitment_no_groups_note))
    ChoiceTile(
        label = stringResource(Res.string.field_dest_group),
        tile = OpenTile.DestGroup,
        open = open,
        onToggle = onToggle,
        preview = state.destGroupName.orEmpty(),
        locked = !creating,
        error = state.destGroupError,
    ) {
        state.groups.forEach { g ->
            FormChip(g.name, state.destGroupId == g.id, enabled) { onEvent(CommitmentFormEvent.DestGroupSelected(g.id)) }
        }
    }
    if (state.destGroupId != null) {
        ChoiceTile(
            label = stringResource(Res.string.field_dest_account),
            tile = OpenTile.DestAccount,
            open = open,
            onToggle = onToggle,
            preview = state.destAccounts.firstOrNull { it.id == state.destAccountId }?.name.orEmpty(),
            locked = !creating,
            error = state.destAccountError,
        ) {
            if (state.isLoadingDestination) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            state.destAccounts.forEach { a ->
                FormChip(a.name, state.destAccountId == a.id, enabled) { onEvent(CommitmentFormEvent.DestAccountSelected(a.id)) }
            }
        }
    }
    ChoiceTile(
        label = stringResource(Res.string.field_source_envelope),
        tile = OpenTile.Envelope,
        open = open,
        onToggle = onToggle,
        preview = state.envelopes.firstOrNull { it.id == state.envelopeId }?.name ?: unassigned,
        locked = !creating,
        error = null,
    ) {
        FormChip(unassigned, state.envelopeId == null, enabled) { onEvent(CommitmentFormEvent.EnvelopeSelected(null)) }
        state.envelopes.forEach { e ->
            FormChip(e.name, state.envelopeId == e.id, enabled) { onEvent(CommitmentFormEvent.EnvelopeSelected(e.id)) }
        }
    }
    if (state.destGroupId != null) {
        ChoiceTile(
            label = stringResource(Res.string.field_dest_envelope),
            tile = OpenTile.DestEnvelope,
            open = open,
            onToggle = onToggle,
            preview = state.destEnvelopes.firstOrNull { it.id == state.destEnvelopeId }?.name ?: unassigned,
            locked = !creating,
            error = null,
        ) {
            FormChip(unassigned, state.destEnvelopeId == null, enabled) { onEvent(CommitmentFormEvent.DestEnvelopeSelected(null)) }
            state.destEnvelopes.forEach { e ->
                FormChip(e.name, state.destEnvelopeId == e.id, enabled) { onEvent(CommitmentFormEvent.DestEnvelopeSelected(e.id)) }
            }
        }
    }
}

private fun FrequencyUnit.labelRes() = when (this) {
    FrequencyUnit.Day -> Res.string.unit_days
    FrequencyUnit.Week -> Res.string.unit_weeks
    FrequencyUnit.Month -> Res.string.unit_months
    FrequencyUnit.Year -> Res.string.unit_years
}

/** "-  N  +" inside a tile, between 1 and 99. */
@Composable
private fun EveryStepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > MIN_EVERY_N) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.every_previous))
        }
        Text(
            text = stringResource(Res.string.every_value, value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        IconButton(onClick = { onChange(value + 1) }, enabled = enabled && value < MAX_EVERY_N) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.every_next))
        }
    }
}

@PreviewLightDark
@Composable
private fun CommitmentFormScreenPreview() {
    SobreioTheme { CommitmentFormScreen(state = CommitmentFormUiState(), onEvent = {}) }
}
