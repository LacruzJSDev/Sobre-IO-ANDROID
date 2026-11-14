package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.generated.resources.field_currency
import dev.lacruzjs.sobreio.generated.resources.field_group_name
import dev.lacruzjs.sobreio.generated.resources.new_group_currency_note
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet to create a group (name and currency) or, when [GroupsUiState.sheet] is Edit, to rename one. The currency of
 * an existing group is shown read-only because it never changes.
 */
@Composable
internal fun GroupFormSheet(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    val editing = state.sheet == GroupsSheet.Edit
    val confirm = if (editing) GroupsEvent.EditConfirmed else GroupsEvent.CreateConfirmed
    FlatBottomSheet(
        title = stringResource(if (editing) Res.string.edit_group_title else Res.string.action_new_group),
        onDismiss = { onEvent(GroupsEvent.SheetDismissed) },
    ) {
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(GroupsEvent.GroupNameChanged(it)) },
            label = stringResource(Res.string.field_group_name),
            errorRes = state.nameError,
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(confirm) },
        )
        if (editing) {
            FormTextField(
                value = state.editingGroup?.currencyCode.orEmpty(),
                onValueChange = {},
                label = stringResource(Res.string.field_currency),
                enabled = false,
            )
        } else {
            Text(text = stringResource(Res.string.field_currency), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCurrencies.forEach { code ->
                    FilterChip(
                        selected = state.currency == code,
                        onClick = { onEvent(GroupsEvent.CurrencySelected(code)) },
                        enabled = !state.isSubmitting,
                        label = { Text(code) },
                        shape = MaterialTheme.shapes.medium,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = null,
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.new_group_currency_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(if (editing) Res.string.action_save else Res.string.action_create),
            onClick = { onEvent(confirm) },
            isLoading = state.isSubmitting,
        )
    }
}
