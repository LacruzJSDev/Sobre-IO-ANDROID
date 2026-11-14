package dev.lacruzjs.sobreio.ui.envelopeform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_envelope
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.edit_envelope_title
import dev.lacruzjs.sobreio.generated.resources.field_envelope_kind
import dev.lacruzjs.sobreio.generated.resources.field_envelope_name
import dev.lacruzjs.sobreio.generated.resources.field_holder
import dev.lacruzjs.sobreio.generated.resources.field_monthly_target
import dev.lacruzjs.sobreio.generated.resources.form_section_appearance
import dev.lacruzjs.sobreio.generated.resources.form_section_envelope
import dev.lacruzjs.sobreio.generated.resources.holder_common
import dev.lacruzjs.sobreio.generated.resources.monthly_target_hint
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
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.HolderPicker
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.IconFormTile
import dev.lacruzjs.sobreio.ui.components.expandedStateText
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.toOption
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
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

/**
 * Same layout as the group form: the avatar next to the name, "Appearance" (colour, icon) and "Envelope" (kind, holder
 * when the group has several members, monthly target with its note).
 */
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
                var open by rememberSaveable { mutableStateOf(OpenTile.None) }
                fun toggle(tile: OpenTile) { open = if (open == tile) OpenTile.None else tile }
                val color = EntityColor.fromHex(state.color)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EntityAvatar(
                        name = state.name,
                        icon = iconFor(IconCategory.Envelope, state.icon),
                        color = color,
                        size = 64.dp,
                    )
                    FormTextField(
                        value = state.name,
                        onValueChange = { onEvent(EnvelopeFormEvent.NameChanged(it)) },
                        label = stringResource(Res.string.field_envelope_name),
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
                        onSelect = { onEvent(EnvelopeFormEvent.ColorSelected(it?.hex)) },
                    )
                    IconFormTile(
                        category = IconCategory.Envelope,
                        selectedKey = state.icon,
                        color = color,
                        expanded = open == OpenTile.Icon,
                        onToggle = { toggle(OpenTile.Icon) },
                        onSelect = { onEvent(EnvelopeFormEvent.IconSelected(it)) },
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_envelope)) {
                    ExpandableFormTile(
                        label = stringResource(Res.string.field_envelope_kind),
                        expanded = open == OpenTile.Kind,
                        onToggle = { toggle(OpenTile.Kind) },
                        expandedDescription = expandedStateText(open == OpenTile.Kind),
                        preview = { FormTileValue(stringResource(state.kind.labelRes())) },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectableKinds.forEach { kind ->
                                FormChip(
                                    label = stringResource(kind.labelRes()),
                                    selected = state.kind == kind,
                                    enabled = !state.isSubmitting,
                                    onClick = { onEvent(EnvelopeFormEvent.KindSelected(kind)) },
                                )
                            }
                        }
                    }
                    if (state.hasSeveralMembers) {
                        val commonLabel = stringResource(Res.string.holder_common)
                        val holderName = state.members.firstOrNull { it.userId == state.ownerUserId }?.displayName
                        ExpandableFormTile(
                            label = stringResource(Res.string.field_holder),
                            expanded = open == OpenTile.Holder,
                            onToggle = { toggle(OpenTile.Holder) },
                            expandedDescription = expandedStateText(open == OpenTile.Holder),
                            preview = { FormTileValue(holderName ?: commonLabel) },
                        ) {
                            HolderPicker(
                                members = state.members.map { it.toOption() },
                                selectedUserId = state.ownerUserId,
                                onSelect = { onEvent(EnvelopeFormEvent.HolderSelected(it)) },
                                enabled = !state.isSubmitting,
                                jointLabel = commonLabel,
                                chipColor = MaterialTheme.colorScheme.background,
                            )
                        }
                    }
                    AmountField(
                        value = state.monthlyTarget,
                        onValueChange = { onEvent(EnvelopeFormEvent.MonthlyTargetChanged(it)) },
                        label = stringResource(Res.string.field_monthly_target),
                        errorRes = state.monthlyTargetError,
                        enabled = !state.isSubmitting,
                    )
                    FormNote(stringResource(Res.string.monthly_target_hint))
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
private fun EnvelopeFormScreenPreview() {
    SobreioTheme { EnvelopeFormScreen(state = EnvelopeFormUiState(), isEditing = false, onEvent = {}) }
}
