package dev.lacruzjs.sobreio.ui.envelopes

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
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.edit_envelope_title
import dev.lacruzjs.sobreio.generated.resources.field_envelope_kind
import dev.lacruzjs.sobreio.generated.resources.field_envelope_name
import dev.lacruzjs.sobreio.generated.resources.field_monthly_target
import dev.lacruzjs.sobreio.generated.resources.monthly_target_hint
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

/** The kinds a user can pick: the system envelope is never created or edited. */
private val selectableKinds = listOf(EnvelopeKind.Spending, EnvelopeKind.Saving)

/** Sheet to create an envelope or, when [EnvelopesUiState.sheet] is Edit, to change it: name, kind and monthly target. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EnvelopeFormSheet(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    val editing = state.sheet == EnvelopesSheet.Edit
    FlatBottomSheet(
        title = stringResource(if (editing) Res.string.edit_envelope_title else Res.string.action_new_envelope),
        onDismiss = { onEvent(EnvelopesEvent.SheetDismissed) },
    ) {
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(EnvelopesEvent.NameChanged(it)) },
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
                    onClick = { onEvent(EnvelopesEvent.KindSelected(kind)) },
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
            onValueChange = { onEvent(EnvelopesEvent.MonthlyTargetChanged(it)) },
            label = stringResource(Res.string.field_monthly_target),
            errorRes = state.monthlyTargetError,
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(EnvelopesEvent.ConfirmClicked) },
        )
        Text(
            text = stringResource(Res.string.monthly_target_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(if (editing) Res.string.action_save else Res.string.action_create),
            onClick = { onEvent(EnvelopesEvent.ConfirmClicked) },
            isLoading = state.isSubmitting,
        )
    }
}
