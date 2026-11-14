package dev.lacruzjs.sobreio.ui.movementform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_new_movement
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.amount_placeholder
import dev.lacruzjs.sobreio.generated.resources.edit_movement_title
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.field_account
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_cover_also
import dev.lacruzjs.sobreio.generated.resources.field_date
import dev.lacruzjs.sobreio.generated.resources.field_dest_account
import dev.lacruzjs.sobreio.generated.resources.field_dest_group
import dev.lacruzjs.sobreio.generated.resources.field_envelope
import dev.lacruzjs.sobreio.generated.resources.field_from
import dev.lacruzjs.sobreio.generated.resources.field_notes
import dev.lacruzjs.sobreio.generated.resources.field_other_group
import dev.lacruzjs.sobreio.generated.resources.field_tag_name
import dev.lacruzjs.sobreio.generated.resources.field_to
import dev.lacruzjs.sobreio.generated.resources.filter_tags
import dev.lacruzjs.sobreio.generated.resources.form_section_details
import dev.lacruzjs.sobreio.generated.resources.form_section_money
import dev.lacruzjs.sobreio.generated.resources.movement_boundary_note
import dev.lacruzjs.sobreio.generated.resources.movement_cover_none
import dev.lacruzjs.sobreio.generated.resources.movement_cover_note
import dev.lacruzjs.sobreio.generated.resources.movement_edit_extra_note
import dev.lacruzjs.sobreio.generated.resources.movement_kind_expense
import dev.lacruzjs.sobreio.generated.resources.movement_kind_income
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer
import dev.lacruzjs.sobreio.generated.resources.movement_no_groups_note
import dev.lacruzjs.sobreio.generated.resources.movement_outside_note
import dev.lacruzjs.sobreio.generated.resources.movement_tag_new
import dev.lacruzjs.sobreio.generated.resources.movement_tag_new_title
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.LargeAmountField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MovementFormRoute(
    groupId: String,
    movementId: String?,
    source: String?,
    accountId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovementFormViewModel = koinViewModel { parametersOf(groupId, movementId, source, accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.saved) {
        LaunchedEffect(Unit) {
            onClose()
            viewModel.onEvent(MovementFormEvent.NavigationHandled)
        }
    }
    MovementFormScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                MovementFormEvent.CloseClicked -> onClose()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/** Which expandable select of the form is open; one at a time keeps the form short. */
private enum class OpenSelect { None, Account, Envelope, Cover, ToAccount, DestGroup, DestAccount }

/**
 * Same layout as the other forms: the kind row and the big amount, then Money (account and envelope, or from and to),
 * Details (date, notes) and, for expenses and incomes, Tags.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovementFormScreen(
    state: MovementFormUiState,
    onEvent: (MovementFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScaffold(
        title = stringResource(if (state.isEditing) Res.string.edit_movement_title else Res.string.action_new_movement),
        onClose = { onEvent(MovementFormEvent.CloseClicked) },
        onSave = { onEvent(MovementFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(MovementFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                var open by rememberSaveable { mutableStateOf(OpenSelect.None) }
                fun toggle(select: OpenSelect) { open = if (open == select) OpenSelect.None else select }
                val languageTag = Locale.current.toLanguageTag()
                val enabled = !state.isSubmitting
                val money = { cents: Long -> formatMoney(cents, state.currencyCode, languageTag) }
                val unassignedName = stringResource(Res.string.envelopes_unassigned)
                val accountOptions = state.accounts.map { it.toEntityOption(money(it.balanceCents)) }

                val amountFocus = remember { FocusRequester() }
                // Creating opens the keypad by itself: the amount is the first thing to type
                LaunchedEffect(state.isEditing) { if (!state.isEditing) runCatching { amountFocus.requestFocus() } }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        EntryKind.Expense to Res.string.movement_kind_expense,
                        EntryKind.Income to Res.string.movement_kind_income,
                        EntryKind.Transfer to Res.string.movement_kind_transfer,
                    ).forEach { (kind, label) ->
                        // The kind of a stored movement is locked
                        FormChip(
                            label = stringResource(label),
                            selected = state.kind == kind,
                            enabled = enabled && !state.isEditing,
                            onClick = { onEvent(MovementFormEvent.KindSelected(kind)) },
                        )
                    }
                }
                LargeAmountField(
                    value = state.amount,
                    onValueChange = { onEvent(MovementFormEvent.AmountChanged(it)) },
                    label = stringResource(Res.string.field_amount),
                    placeholder = stringResource(Res.string.amount_placeholder),
                    currencySymbol = if (state.currencyCode == "EUR") "€" else state.currencyCode,
                    errorRes = state.amountError,
                    enabled = enabled,
                    focusRequester = amountFocus,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                FormSection(title = stringResource(Res.string.form_section_money)) {
                    EntitySelect(
                        label = stringResource(if (state.isTransfer) Res.string.field_from else Res.string.field_account),
                        options = accountOptions,
                        selectedId = state.accountId,
                        onSelect = { onEvent(MovementFormEvent.AccountSelected(it)) },
                        expanded = open == OpenSelect.Account,
                        onToggle = { toggle(OpenSelect.Account) },
                        enabled = enabled,
                        error = state.accountError,
                    )
                    if (state.isTransfer) {
                        TransferTiles(state, open, ::toggle, onEvent, accountOptions, enabled)
                    } else if (state.showsEnvelope) {
                        EnvelopeTiles(state, open, ::toggle, onEvent, unassignedName, money, enabled)
                    } else {
                        FormNote(stringResource(Res.string.movement_outside_note))
                    }
                }

                FormSection(title = stringResource(Res.string.form_section_details)) {
                    DateFormTile(
                        label = stringResource(Res.string.field_date),
                        date = state.date,
                        onDateSelected = { onEvent(MovementFormEvent.DateChanged(it)) },
                        enabled = enabled,
                        maxDate = state.today,
                    )
                    FormTextField(
                        value = state.notes,
                        onValueChange = { onEvent(MovementFormEvent.NotesChanged(it)) },
                        label = stringResource(Res.string.field_notes),
                        enabled = enabled,
                        imeAction = ImeAction.Done,
                    )
                }

                if (!state.isTransfer) {
                    FormSection(title = stringResource(Res.string.filter_tags)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.tags.forEach { tag ->
                                FormChip(
                                    label = tag.name,
                                    selected = tag.id in state.selectedTagIds,
                                    enabled = enabled,
                                    onClick = { onEvent(MovementFormEvent.TagToggled(tag.id)) },
                                )
                            }
                            FormChip(
                                label = stringResource(Res.string.movement_tag_new),
                                selected = false,
                                enabled = enabled,
                                onClick = { onEvent(MovementFormEvent.NewTagClicked) },
                            )
                        }
                    }
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())

                if (state.newTagOpen) NewTagSheet(state, onEvent)
            }
        }
    }
}

/** Envelope of an expense or income, with the cover note and its optional second source. */
@Composable
private fun EnvelopeTiles(
    state: MovementFormUiState,
    open: OpenSelect,
    onToggle: (OpenSelect) -> Unit,
    onEvent: (MovementFormEvent) -> Unit,
    unassignedName: String,
    money: (Long) -> String,
    enabled: Boolean,
) {
    // "Unassigned" is the first option, with its real id: no envelope chosen is a null selection, not "Unassigned"
    val unassigned = state.unassigned
    val options = listOfNotNull(
        unassigned?.let { EntityOption(it.id, unassignedName, Icons.Filled.Inbox, trailing = money(it.balanceCents)) },
    ) + state.envelopes.filter { it.kind != EnvelopeKind.System }.sortedBy { it.name.lowercase() }
        .map { it.toEntityOption(money(it.balanceCents)) }
    EntitySelect(
        label = stringResource(Res.string.field_envelope),
        options = options,
        selectedId = state.envelopeId,
        onSelect = { onEvent(MovementFormEvent.EnvelopeSelected(it)) },
        expanded = open == OpenSelect.Envelope,
        onToggle = { onToggle(OpenSelect.Envelope) },
        enabled = enabled,
        error = state.envelopeError,
    )
    val chosen = state.chosenEnvelope
    if (state.showsCoverNote && chosen != null) {
        FormNote(stringResource(Res.string.movement_cover_note, chosen.name, money(chosen.balanceCents)))
        if (state.coverCandidates.isNotEmpty()) {
            EntitySelect(
                label = stringResource(Res.string.field_cover_also),
                options = listOf(EntityOption(null, stringResource(Res.string.movement_cover_none))) +
                    state.coverCandidates.map { it.toEntityOption(money(it.balanceCents)) },
                selectedId = state.coverEnvelopeId,
                onSelect = { onEvent(MovementFormEvent.CoverSelected(it)) },
                expanded = open == OpenSelect.Cover,
                onToggle = { onToggle(OpenSelect.Cover) },
                enabled = enabled,
            )
        }
    }
    if (state.showsEditExtraNote) FormNote(stringResource(Res.string.movement_edit_extra_note))
}

/** "To" (or the destination group and account), the other-group switch and the boundary note. */
@Composable
private fun TransferTiles(
    state: MovementFormUiState,
    open: OpenSelect,
    onToggle: (OpenSelect) -> Unit,
    onEvent: (MovementFormEvent) -> Unit,
    accountOptions: List<EntityOption>,
    enabled: Boolean,
) {
    if (!state.isEditing) {
        FormTile(label = stringResource(Res.string.field_other_group)) {
            Switch(
                checked = state.crossGroup,
                onCheckedChange = { onEvent(MovementFormEvent.CrossGroupChanged(it)) },
                enabled = enabled,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
    if (state.crossGroup) {
        if (state.groups.isEmpty()) FormNote(stringResource(Res.string.movement_no_groups_note))
        EntitySelect(
            label = stringResource(Res.string.field_dest_group),
            options = state.groups.map { it.toEntityOption() },
            selectedId = state.destGroupId,
            onSelect = { onEvent(MovementFormEvent.DestGroupSelected(it)) },
            expanded = open == OpenSelect.DestGroup,
            onToggle = { onToggle(OpenSelect.DestGroup) },
            enabled = enabled,
            error = state.destGroupError,
        )
        if (state.destGroupId != null) {
            EntitySelect(
                label = stringResource(Res.string.field_dest_account),
                options = state.destAccounts.map { it.toEntityOption() },
                selectedId = state.destAccountId,
                onSelect = { onEvent(MovementFormEvent.DestAccountSelected(it)) },
                expanded = open == OpenSelect.DestAccount,
                onToggle = { onToggle(OpenSelect.DestAccount) },
                enabled = enabled,
                error = state.destAccountError,
            )
            if (state.isLoadingDestination) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
        }
    } else {
        EntitySelect(
            label = stringResource(Res.string.field_to),
            options = accountOptions.filter { it.id != state.accountId },
            selectedId = state.toAccountId,
            onSelect = { onEvent(MovementFormEvent.ToAccountSelected(it)) },
            expanded = open == OpenSelect.ToAccount,
            onToggle = { onToggle(OpenSelect.ToAccount) },
            enabled = enabled,
            error = state.toAccountError,
        )
        if (state.passesThroughUnassigned) FormNote(stringResource(Res.string.movement_boundary_note))
    }
}

/** Short one-step sheet to create a tag, which is selected at once. */
@Composable
private fun NewTagSheet(state: MovementFormUiState, onEvent: (MovementFormEvent) -> Unit) {
    FlatBottomSheet(
        title = stringResource(Res.string.movement_tag_new_title),
        onDismiss = { onEvent(MovementFormEvent.NewTagDismissed) },
    ) {
        FormTextField(
            value = state.newTagName,
            onValueChange = { onEvent(MovementFormEvent.NewTagNameChanged(it)) },
            label = stringResource(Res.string.field_tag_name),
            errorRes = state.newTagError,
            enabled = !state.isCreatingTag,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(MovementFormEvent.NewTagConfirmed) },
        )
        ErrorText(state.newTagFailure, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_create),
            onClick = { onEvent(MovementFormEvent.NewTagConfirmed) },
            isLoading = state.isCreatingTag,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewLightDark
@Composable
private fun MovementFormScreenPreview() {
    SobreioTheme { MovementFormScreen(state = MovementFormUiState(isLoading = false), onEvent = {}) }
}
