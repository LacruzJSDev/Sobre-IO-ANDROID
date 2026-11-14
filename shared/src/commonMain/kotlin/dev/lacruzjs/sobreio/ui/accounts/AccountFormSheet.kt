package dev.lacruzjs.sobreio.ui.accounts

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.edit_account_title
import dev.lacruzjs.sobreio.generated.resources.field_account_kind
import dev.lacruzjs.sobreio.generated.resources.field_account_name
import dev.lacruzjs.sobreio.generated.resources.field_opening_balance
import dev.lacruzjs.sobreio.generated.resources.opening_balance_hint
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet to create an account (name, kind and opening balance) or, when [AccountsUiState.sheet] is Edit, to change its
 * name and kind. The opening balance exists only at creation: later changes are movements.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountFormSheet(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    val editing = state.sheet == AccountsSheet.Edit
    FlatBottomSheet(
        title = stringResource(if (editing) Res.string.edit_account_title else Res.string.action_new_account),
        onDismiss = { onEvent(AccountsEvent.SheetDismissed) },
    ) {
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(AccountsEvent.NameChanged(it)) },
            label = stringResource(Res.string.field_account_name),
            errorRes = state.nameError,
            enabled = !state.isSubmitting,
            imeAction = if (editing) ImeAction.Done else ImeAction.Next,
            onImeAction = { if (editing) onEvent(AccountsEvent.ConfirmClicked) },
        )
        Text(text = stringResource(Res.string.field_account_kind), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { onEvent(AccountsEvent.KindSelected(kind)) },
                    enabled = !state.isSubmitting,
                    label = { Text(stringResource(kind.labelRes())) },
                    shape = MaterialTheme.shapes.medium,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = null,
                )
            }
        }
        if (!editing) {
            // The plain text keyboard is used on purpose: the decimal pads of some platforms have no minus sign
            FormTextField(
                value = state.openingBalance,
                onValueChange = { onEvent(AccountsEvent.OpeningBalanceChanged(it)) },
                label = stringResource(Res.string.field_opening_balance),
                errorRes = state.openingBalanceError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(AccountsEvent.ConfirmClicked) },
            )
            Text(
                text = stringResource(Res.string.opening_balance_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(if (editing) Res.string.action_save else Res.string.action_create),
            onClick = { onEvent(AccountsEvent.ConfirmClicked) },
            isLoading = state.isSubmitting,
        )
    }
}
