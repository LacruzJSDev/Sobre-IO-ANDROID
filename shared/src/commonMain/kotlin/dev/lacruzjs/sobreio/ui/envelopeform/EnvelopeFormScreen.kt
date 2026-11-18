package dev.lacruzjs.sobreio.ui.envelopeform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
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
import dev.lacruzjs.sobreio.generated.resources.field_monthly_contribution
import dev.lacruzjs.sobreio.generated.resources.field_envelope_start_next_period
import dev.lacruzjs.sobreio.generated.resources.envelope_start_next_period_note
import dev.lacruzjs.sobreio.generated.resources.envelope_start_next_period_note_off
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.components.ColorFormTile
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ChipFormTile
import dev.lacruzjs.sobreio.ui.components.FormChip
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.groupsettings.labelRes
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.IconFormTile
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import dev.lacruzjs.sobreio.ui.envelopes.noteRes
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The four types of envelope (D-46), in the order the choice shows them. */
private val purposes = EnvelopePurpose.entries

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
 * Same layout as the group form: the avatar next to the name, "Appearance" (colour, icon) and "Envelope": the type as a
 * row of chips, the holder when the group has several members and, for Monthly and Savings, the amount. The note that
 * explains the chosen type comes last.
 */
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
                        capitalization = KeyboardCapitalization.Sentences,
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
                    ChipFormTile(label = stringResource(Res.string.field_envelope_kind), error = state.purposeError) {
                        purposes.forEach { purpose ->
                            FormChip(
                                label = stringResource(purpose.labelRes()),
                                selected = state.purpose == purpose,
                                enabled = !state.isSubmitting,
                                onClick = { onEvent(EnvelopeFormEvent.PurposeSelected(purpose)) },
                            )
                        }
                    }
                    if (state.hasSeveralMembers) {
                        val noneOption = EntityOption(
                            id = null,
                            name = stringResource(Res.string.holder_common),
                            icon = Icons.Filled.Group,
                        )
                        val memberOptions = state.members.map { it.toEntityOption(stringResource(it.role.labelRes())) }
                        EntitySelect(
                            label = stringResource(Res.string.field_holder),
                            options = listOf(noneOption) + memberOptions,
                            selectedId = state.ownerUserId,
                            onSelect = { onEvent(EnvelopeFormEvent.HolderSelected(it)) },
                            expanded = open == OpenTile.Holder,
                            onToggle = { toggle(OpenTile.Holder) },
                            enabled = !state.isSubmitting,
                        )
                    }
                    if (state.showsTarget) {
                        AmountField(
                            value = state.monthlyTarget,
                            onValueChange = { onEvent(EnvelopeFormEvent.MonthlyTargetChanged(it)) },
                            label = stringResource(
                                if (state.targetRequired) Res.string.field_monthly_target else Res.string.field_monthly_contribution,
                            ),
                            errorRes = state.monthlyTargetError,
                            enabled = !state.isSubmitting,
                        )
                    }
                    // D-53: a new target can wait for the next period instead of taking money from Unassigned now
                    val nextStart = state.nextPeriodStart
                    if (state.canChooseStart && nextStart != null) {
                        val languageTag = Locale.current.toLanguageTag()
                        FormTile(
                            label = stringResource(Res.string.field_envelope_start_next_period),
                            note = stringResource(
                                if (state.startsNextPeriod) {
                                    Res.string.envelope_start_next_period_note
                                } else {
                                    Res.string.envelope_start_next_period_note_off
                                },
                                formatDate(nextStart, languageTag),
                            ),
                        ) {
                            Switch(
                                checked = state.startsNextPeriod,
                                onCheckedChange = { onEvent(EnvelopeFormEvent.StartNextPeriodChanged(it)) },
                                enabled = !state.isSubmitting,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
                // What the chosen type does, at the end so the choices above stay close together
                FormNote(stringResource(state.purpose.noteRes()))
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Which expandable tile of the form is open; one at a time keeps the form short. */
private enum class OpenTile { None, Color, Icon, Holder }

@PreviewLightDark
@Composable
private fun EnvelopeFormScreenPreview() {
    SobreioTheme { EnvelopeFormScreen(state = EnvelopeFormUiState(), isEditing = false, onEvent = {}) }
}
