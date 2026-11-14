package dev.lacruzjs.sobreio.ui.accountform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_account
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.edit_account_title
import dev.lacruzjs.sobreio.generated.resources.field_account_kind
import dev.lacruzjs.sobreio.generated.resources.field_account_name
import dev.lacruzjs.sobreio.generated.resources.field_opening_balance
import dev.lacruzjs.sobreio.generated.resources.opening_balance_hint
import dev.lacruzjs.sobreio.ui.accounts.labelRes
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormTextField
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

/** Name and kind, plus the opening balance when creating: the opening balance exists only at creation. */
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
                FormTextField(
                    value = state.name,
                    onValueChange = { onEvent(AccountFormEvent.NameChanged(it)) },
                    label = stringResource(Res.string.field_account_name),
                    errorRes = state.nameError,
                    enabled = !state.isSubmitting,
                    imeAction = if (isEditing) ImeAction.Done else ImeAction.Next,
                    onImeAction = { if (isEditing) onEvent(AccountFormEvent.SaveClicked) },
                )
                Text(text = stringResource(Res.string.field_account_kind), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.kind == kind,
                            onClick = { onEvent(AccountFormEvent.KindSelected(kind)) },
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
                if (!isEditing) {
                    // The plain text keyboard is used on purpose: the decimal pads of some platforms have no minus sign
                    FormTextField(
                        value = state.openingBalance,
                        onValueChange = { onEvent(AccountFormEvent.OpeningBalanceChanged(it)) },
                        label = stringResource(Res.string.field_opening_balance),
                        errorRes = state.openingBalanceError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        onImeAction = { onEvent(AccountFormEvent.SaveClicked) },
                    )
                    Text(
                        text = stringResource(Res.string.opening_balance_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
