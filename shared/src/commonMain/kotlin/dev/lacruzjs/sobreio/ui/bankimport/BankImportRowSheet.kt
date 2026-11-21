package dev.lacruzjs.sobreio.ui.bankimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_create
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_confirm
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_ignore
import dev.lacruzjs.sobreio.generated.resources.bank_import_action_link
import dev.lacruzjs.sobreio.generated.resources.bank_import_before_opening_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_from_bank
import dev.lacruzjs.sobreio.generated.resources.bank_import_ignore_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_outside_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_similar_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_what_to_do
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.bank_import_done
import dev.lacruzjs.sobreio.generated.resources.field_envelope
import dev.lacruzjs.sobreio.generated.resources.field_from_account
import dev.lacruzjs.sobreio.generated.resources.field_notes
import dev.lacruzjs.sobreio.generated.resources.field_to_account
import dev.lacruzjs.sobreio.generated.resources.bank_import_field_due
import dev.lacruzjs.sobreio.generated.resources.bank_import_field_movement
import dev.lacruzjs.sobreio.generated.resources.bank_import_field_kind
import dev.lacruzjs.sobreio.generated.resources.movement_kind_expense
import dev.lacruzjs.sobreio.generated.resources.movement_kind_income
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer
import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.NoticeTone
import dev.lacruzjs.sobreio.ui.components.ChipFormTile
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.components.unassignedOption
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Short one-step sheet for one statement row: what to do with it and, for a new row, its kind, envelope (or the other
 * account of a transfer) and notes. Date and amount come from the bank and are not edited.
 */
@Composable
internal fun RowSheet(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val sheet = state.sheet ?: return
    val draft = sheet.draft
    val languageTag = Locale.current.toLanguageTag()
    val year = state.today?.year ?: 0
    val isCharge = draft.row.amountCents < 0
    FlatBottomSheet(title = draft.row.description, onDismiss = { onEvent(BankImportEvent.SheetDismissed) }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.bank_import_from_bank, formatShortDate(draft.row.date, languageTag, year)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            MoneyText(draft.row.amountCents, state.currencyCode, MoneyRole.Flow, style = MaterialTheme.typography.titleMedium)
        }
        ChipFormTile(label = stringResource(Res.string.bank_import_what_to_do)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormChip(stringResource(Res.string.bank_import_action_create), draft.action == BankRowAction.Create, true) {
                    onEvent(BankImportEvent.SheetActionChosen(BankRowAction.Create))
                }
                if (draft.pending.isNotEmpty()) {
                    FormChip(stringResource(Res.string.bank_import_action_confirm), draft.action == BankRowAction.Confirm, true) {
                        onEvent(BankImportEvent.SheetActionChosen(BankRowAction.Confirm))
                    }
                }
                if (draft.existing.isNotEmpty()) {
                    FormChip(stringResource(Res.string.bank_import_action_link), draft.action == BankRowAction.Link, true) {
                        onEvent(BankImportEvent.SheetActionChosen(BankRowAction.Link))
                    }
                }
                FormChip(stringResource(Res.string.bank_import_action_ignore), draft.action == BankRowAction.Ignore, true) {
                    onEvent(BankImportEvent.SheetActionChosen(BankRowAction.Ignore))
                }
            }
        }
        when (draft.action) {
            BankRowAction.Create -> {
                if (state.otherAccounts.isNotEmpty()) {
                    ChipFormTile(label = stringResource(Res.string.bank_import_field_kind)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FormChip(
                                stringResource(if (isCharge) Res.string.movement_kind_expense else Res.string.movement_kind_income),
                                !draft.isTransfer,
                                true,
                            ) { onEvent(BankImportEvent.SheetTransferChosen(false)) }
                            FormChip(stringResource(Res.string.movement_kind_transfer), draft.isTransfer, true) {
                                onEvent(BankImportEvent.SheetTransferChosen(true))
                            }
                        }
                    }
                }
                when {
                    draft.isTransfer -> EntitySelect(
                        label = stringResource(if (isCharge) Res.string.field_to_account else Res.string.field_from_account),
                        options = state.otherAccounts.map { it.toEntityOption() },
                        selectedId = draft.otherAccountId,
                        onSelect = { onEvent(BankImportEvent.SheetOtherAccountChosen(it)) },
                        expanded = sheet.openSelect == RowSelect.OtherAccount,
                        onToggle = { onEvent(BankImportEvent.SheetSelectToggled(RowSelect.OtherAccount)) },
                    )
                    state.account?.inEnvelopes == false -> InlineNotice(stringResource(Res.string.bank_import_outside_note))
                    else -> {
                        EntitySelect(
                            label = stringResource(Res.string.field_envelope),
                            options = listOf(unassignedOption(stringResource(Res.string.envelopes_unassigned))) +
                                state.envelopes.map { it.toEntityOption() },
                            selectedId = draft.envelopeId,
                            onSelect = { onEvent(BankImportEvent.SheetEnvelopeChosen(it)) },
                            expanded = sheet.openSelect == RowSelect.Envelope,
                            onToggle = { onEvent(BankImportEvent.SheetSelectToggled(RowSelect.Envelope)) },
                        )
                        if (sheet.similarCount > 0) {
                            InlineNotice(pluralStringResource(Res.plurals.bank_import_similar_note, sheet.similarCount, sheet.similarCount))
                        }
                    }
                }
                FormTextField(
                    value = draft.notes,
                    onValueChange = { onEvent(BankImportEvent.SheetNotesChanged(it)) },
                    label = stringResource(Res.string.field_notes),
                    capitalization = KeyboardCapitalization.Sentences,
                )
                if (draft.status == BankRowStatus.BeforeOpening) InlineNotice(stringResource(Res.string.bank_import_before_opening_note), tone = NoticeTone.Warning)
            }
            BankRowAction.Confirm -> EntitySelect(
                label = stringResource(Res.string.bank_import_field_due),
                options = draft.pending.map {
                    EntityOption(
                        id = pendingKey(it.commitmentId, it.dueDate.toString()),
                        name = "${it.name} · ${formatShortDate(it.dueDate, languageTag, year)}",
                        trailing = formatMoney(it.amountCents, state.currencyCode, languageTag),
                    )
                },
                selectedId = draft.commitmentId?.let { id -> draft.dueDate?.let { pendingKey(id, it.toString()) } },
                onSelect = { onEvent(BankImportEvent.SheetPendingChosen(it)) },
                expanded = sheet.openSelect == RowSelect.Pending,
                onToggle = { onEvent(BankImportEvent.SheetSelectToggled(RowSelect.Pending)) },
            )
            BankRowAction.Link -> EntitySelect(
                label = stringResource(Res.string.bank_import_field_movement),
                options = draft.existing.map {
                    EntityOption(
                        id = it.id,
                        name = it.notes?.takeIf(String::isNotBlank) ?: draft.row.description,
                        trailing = formatShortDate(it.date, languageTag, year),
                    )
                },
                selectedId = draft.linkId,
                onSelect = { onEvent(BankImportEvent.SheetExistingChosen(it)) },
                expanded = sheet.openSelect == RowSelect.Existing,
                onToggle = { onEvent(BankImportEvent.SheetSelectToggled(RowSelect.Existing)) },
            )
            BankRowAction.Ignore -> InlineNotice(stringResource(Res.string.bank_import_ignore_note))
        }
        PrimaryButton(text = stringResource(Res.string.bank_import_done), onClick = { onEvent(BankImportEvent.SheetDoneClicked) })
    }
}
