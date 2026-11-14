package dev.lacruzjs.sobreio.ui.groupform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.appearance_field_label
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.generated.resources.field_currency
import dev.lacruzjs.sobreio.generated.resources.field_group_name
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.new_group_currency_note
import dev.lacruzjs.sobreio.ui.components.ColorPicker
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.IconPicker
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.groups.GroupCurrencies
import dev.lacruzjs.sobreio.ui.groups.PeriodDayField
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * [pickedTimeZone] is the id the time zone picker returned (null when none is waiting); the form takes it and tells the
 * caller through [onTimeZoneConsumed], so it is applied once. [onSaved] gets the id of the group created or saved.
 */
@Composable
fun GroupFormRoute(
    groupId: String?,
    pickedTimeZone: String?,
    onTimeZoneConsumed: () -> Unit,
    onClose: () -> Unit,
    onPickTimeZone: (current: String) -> Unit,
    onSaved: (groupId: String, created: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupFormViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    pickedTimeZone?.let { id ->
        LaunchedEffect(id) {
            viewModel.onEvent(GroupFormEvent.TimeZonePicked(id))
            onTimeZoneConsumed()
        }
    }
    state.savedGroupId?.let { savedId ->
        LaunchedEffect(savedId) {
            onSaved(savedId, groupId == null)
            viewModel.onEvent(GroupFormEvent.NavigationHandled)
        }
    }

    GroupFormScreen(
        state = state,
        isEditing = groupId != null,
        onEvent = { event ->
            when (event) {
                GroupFormEvent.CloseClicked -> onClose()
                GroupFormEvent.TimeZoneClicked -> onPickTimeZone(state.form.timezone)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun GroupFormScreen(
    state: GroupFormUiState,
    isEditing: Boolean,
    onEvent: (GroupFormEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = state.form
    FormScaffold(
        title = stringResource(if (isEditing) Res.string.edit_group_title else Res.string.action_new_group),
        onClose = { onEvent(GroupFormEvent.CloseClicked) },
        onSave = { onEvent(GroupFormEvent.SaveClicked) },
        isSaving = state.isSubmitting,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> {
                ErrorText(state.loadError, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { onEvent(GroupFormEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
            }
            else -> {
                FormTextField(
                    value = form.name,
                    onValueChange = { onEvent(GroupFormEvent.FormChanged(form.copy(name = it, nameError = null))) },
                    label = stringResource(Res.string.field_group_name),
                    errorRes = form.nameError,
                    enabled = !state.isSubmitting,
                    imeAction = ImeAction.Done,
                    onImeAction = { onEvent(GroupFormEvent.SaveClicked) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(Res.string.appearance_field_label), style = MaterialTheme.typography.labelLarge)
                    EntityAvatar(
                        name = form.name,
                        icon = iconFor(IconCategory.Group, form.icon),
                        color = EntityColor.fromHex(form.color),
                        size = 56.dp,
                    )
                    ColorPicker(
                        selected = EntityColor.fromHex(form.color),
                        onSelect = { onEvent(GroupFormEvent.FormChanged(form.copy(color = it?.hex))) },
                    )
                    IconPicker(
                        category = IconCategory.Group,
                        selected = form.icon,
                        color = EntityColor.fromHex(form.color),
                        onSelect = { onEvent(GroupFormEvent.FormChanged(form.copy(icon = it))) },
                    )
                }
                if (isEditing) {
                    FormTextField(
                        value = form.currency,
                        onValueChange = {},
                        label = stringResource(Res.string.field_currency),
                        enabled = false,
                    )
                } else {
                    Text(text = stringResource(Res.string.field_currency), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GroupCurrencies.forEach { code ->
                            FilterChip(
                                selected = form.currency == code,
                                onClick = { onEvent(GroupFormEvent.FormChanged(form.copy(currency = code))) },
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
                    Text(
                        text = stringResource(Res.string.new_group_currency_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PeriodDayField(
                    day = form.periodStartDay,
                    onChange = { onEvent(GroupFormEvent.FormChanged(form.copy(periodStartDay = it))) },
                    enabled = !state.isSubmitting,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(Res.string.field_time_zone), style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = form.timezone,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = !state.isSubmitting, role = Role.Button) {
                                onEvent(GroupFormEvent.TimeZoneClicked)
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GroupFormScreenPreview() {
    SobreioTheme { GroupFormScreen(state = GroupFormUiState(), isEditing = false, onEvent = {}) }
}
