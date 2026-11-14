package dev.lacruzjs.sobreio.ui.envelopeform

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
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.edit_envelope_title
import dev.lacruzjs.sobreio.generated.resources.field_envelope_kind
import dev.lacruzjs.sobreio.generated.resources.field_envelope_name
import dev.lacruzjs.sobreio.generated.resources.field_monthly_target
import dev.lacruzjs.sobreio.generated.resources.monthly_target_hint
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The kinds a user can pick: the system envelope is never created or edited. */
private val selectableKinds = listOf(EnvelopeKind.Spending, EnvelopeKind.Saving)

@Composable
fun EnvelopeFormRoute(
    groupId: String,
    envelopeId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvelopeFormViewModel = koinViewModel { parametersOf(groupId, envelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.saved) {
        LaunchedEffect(Unit) {
            onClose()
            viewModel.onEvent(EnvelopeFormEvent.NavigationHandled)
        }
    }
    EnvelopeFormScreen(
        state = state,
        isEditing = envelopeId != null,
        onEvent = { event ->
            when (event) {
                EnvelopeFormEvent.CloseClicked -> onClose()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvelopeFormScreen(
    state: EnvelopeFormUiState,
    isEditing: Boolean,
    onEvent: (EnvelopeFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormScaffold(
        title = stringResource(if (isEditing) Res.string.edit_envelope_title else Res.string.action_new_envelope),
        onClose = { onEvent(EnvelopeFormEvent.CloseClicked) },
        onSave = { onEvent(EnvelopeFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(EnvelopeFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                FormTextField(
                    value = state.name,
                    onValueChange = { onEvent(EnvelopeFormEvent.NameChanged(it)) },
                    label = stringResource(Res.string.field_envelope_name),
                    errorRes = state.nameError,
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Next,
                )
                Text(text = stringResource(Res.string.field_envelope_kind), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectableKinds.forEach { kind ->
                        FilterChip(
                            selected = state.kind == kind,
                            onClick = { onEvent(EnvelopeFormEvent.KindSelected(kind)) },
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
                FormTextField(
                    value = state.monthlyTarget,
                    onValueChange = { onEvent(EnvelopeFormEvent.MonthlyTargetChanged(it)) },
                    label = stringResource(Res.string.field_monthly_target),
                    errorRes = state.monthlyTargetError,
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Done,
                    onImeAction = { onEvent(EnvelopeFormEvent.SaveClicked) },
                )
                Text(
                    text = stringResource(Res.string.monthly_target_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
