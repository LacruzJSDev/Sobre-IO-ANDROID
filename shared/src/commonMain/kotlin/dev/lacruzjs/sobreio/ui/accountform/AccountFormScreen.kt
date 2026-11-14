package dev.lacruzjs.sobreio.ui.accountform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_in_envelopes_note
import dev.lacruzjs.sobreio.generated.resources.account_scope_edit_note
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.edit_account_title
import dev.lacruzjs.sobreio.generated.resources.field_account_kind
import dev.lacruzjs.sobreio.generated.resources.field_account_name
import dev.lacruzjs.sobreio.generated.resources.field_holder
import dev.lacruzjs.sobreio.generated.resources.field_in_envelopes
import dev.lacruzjs.sobreio.generated.resources.field_opening_balance
import dev.lacruzjs.sobreio.generated.resources.form_section_account
import dev.lacruzjs.sobreio.generated.resources.form_section_appearance
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.generated.resources.opening_balance_hint
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.ColorFormTile
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ExpandableFormTile
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.HolderPicker
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.IconFormTile
import dev.lacruzjs.sobreio.ui.components.expandedStateText
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.toOption
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AccountFormRoute(
    groupId: String,
    accountId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountFormViewModel = koinViewModel { parametersOf(groupId, accountId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.saved) {
        LaunchedEffect(Unit) {
            onClose()
            viewModel.onEvent(AccountFormEvent.NavigationHandled)
        }
    }
    AccountFormScreen(
        state = state,
        isEditing = accountId != null,
        onEvent = { event ->
            when (event) {
                AccountFormEvent.CloseClicked -> onClose()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * Same layout as the group form: the avatar next to the name, "Appearance" (colour, icon) and "Account" (kind, holder
 * when the group has several members, whether it shares its money into the envelopes and, only when creating, the
 * opening balance).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFormScreen(
    state: AccountFormUiState,
    isEditing: Boolean,
    onEvent: (AccountFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScaffold(
        title = stringResource(if (isEditing) Res.string.edit_account_title else Res.string.action_new_account),
        onClose = { onEvent(AccountFormEvent.CloseClicked) },
        onSave = { onEvent(AccountFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(AccountFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                var open by rememberSaveable { mutableStateOf(OpenTile.None) }
                fun toggle(tile: OpenTile) { open = if (open == tile) OpenTile.None else tile }
                val color = EntityColor.fromHex(state.color)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EntityAvatar(
                        name = state.name,
                        icon = iconFor(IconCategory.Account, state.icon),
                        color = color,
                        size = 64.dp,
                    )
                    FormTextField(
                        value = state.name,
                        onValueChange = { onEvent(AccountFormEvent.NameChanged(it)) },
                        label = stringResource(Res.string.field_account_name),
                        errorRes = state.nameError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        modifier = Modifier.weight(1f),
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_appearance)) {
                    ColorFormTile(
                        selected = color,
                        expanded = open == OpenTile.Color,
                        onToggle = { toggle(OpenTile.Color) },
                        onSelect = { onEvent(AccountFormEvent.ColorSelected(it?.hex)) },
                    )
                    IconFormTile(
                        category = IconCategory.Account,
                        selectedKey = state.icon,
                        color = color,
                        expanded = open == OpenTile.Icon,
                        onToggle = { toggle(OpenTile.Icon) },
                        onSelect = { onEvent(AccountFormEvent.IconSelected(it)) },
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_account)) {
                    ExpandableFormTile(
                        label = stringResource(Res.string.field_account_kind),
                        expanded = open == OpenTile.Kind,
                        onToggle = { toggle(OpenTile.Kind) },
                        expandedDescription = expandedStateText(open == OpenTile.Kind),
                        preview = { FormTileValue(stringResource(state.kind.labelRes())) },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccountKind.entries.forEach { kind ->
                                FormChip(
                                    label = stringResource(kind.labelRes()),
                                    selected = state.kind == kind,
                                    enabled = !state.isSubmitting,
                                    onClick = { onEvent(AccountFormEvent.KindSelected(kind)) },
                                )
                            }
                        }
                    }
                    if (state.hasSeveralMembers) {
                        val jointLabel = stringResource(Res.string.holder_joint)
                        val holderName = state.members.firstOrNull { it.userId == state.ownerUserId }?.displayName
                        ExpandableFormTile(
                            label = stringResource(Res.string.field_holder),
                            expanded = open == OpenTile.Holder,
                            onToggle = { toggle(OpenTile.Holder) },
                            expandedDescription = expandedStateText(open == OpenTile.Holder),
                            preview = { FormTileValue(holderName ?: jointLabel) },
                        ) {
                            HolderPicker(
                                members = state.members.map { it.toOption() },
                                selectedUserId = state.ownerUserId,
                                onSelect = { onEvent(AccountFormEvent.HolderSelected(it)) },
                                enabled = !state.isSubmitting,
                                chipColor = MaterialTheme.colorScheme.background,
                            )
                        }
                    }
                    val scopeNote = stringResource(Res.string.account_in_envelopes_note) +
                        if (isEditing) " " + stringResource(Res.string.account_scope_edit_note) else ""
                    FormTile(label = stringResource(Res.string.field_in_envelopes), note = scopeNote) {
                        Switch(
                            checked = state.inEnvelopes,
                            onCheckedChange = { onEvent(AccountFormEvent.InEnvelopesChanged(it)) },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    state.scopeError?.let { error ->
                        Text(
                            text = stringResource(error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    if (!isEditing) {
                        // Numeric keypad; a debt (credit card) is entered with the ± button, as the pad has no minus key
                        AmountField(
                            value = state.openingBalance,
                            onValueChange = { onEvent(AccountFormEvent.OpeningBalanceChanged(it)) },
                            label = stringResource(Res.string.field_opening_balance),
                            errorRes = state.openingBalanceError,
                            enabled = !state.isSubmitting,
                            allowNegative = true,
                        )
                        FormNote(stringResource(Res.string.opening_balance_hint))
                    }
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Which expandable tile of the form is open; one at a time keeps the form short. */
private enum class OpenTile { None, Color, Icon, Kind, Holder }

@PreviewLightDark
@Composable
private fun AccountFormScreenPreview() {
    SobreioTheme { AccountFormScreen(state = AccountFormUiState(), isEditing = false, onEvent = {}) }
}
