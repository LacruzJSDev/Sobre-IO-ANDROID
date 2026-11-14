package dev.lacruzjs.sobreio.ui.groupform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import dev.lacruzjs.sobreio.generated.resources.currency_locked_note
import dev.lacruzjs.sobreio.generated.resources.field_period_start_short
import dev.lacruzjs.sobreio.generated.resources.form_section_appearance
import dev.lacruzjs.sobreio.generated.resources.form_section_currency_period
import dev.lacruzjs.sobreio.generated.resources.period_day_next
import dev.lacruzjs.sobreio.generated.resources.period_day_previous
import dev.lacruzjs.sobreio.generated.resources.period_day_value
import dev.lacruzjs.sobreio.generated.resources.period_start_day_note
import dev.lacruzjs.sobreio.ui.components.ExpandableFormTile
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.groups.MAX_PERIOD_START_DAY
import dev.lacruzjs.sobreio.ui.groups.MIN_PERIOD_START_DAY
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.generated.resources.field_currency
import dev.lacruzjs.sobreio.generated.resources.field_group_name
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.new_group_currency_note
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ColorFormTile
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconFormTile
import dev.lacruzjs.sobreio.ui.components.expandedStateText
import dev.lacruzjs.sobreio.ui.components.FormScaffold
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.groups.GroupCurrencies
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
                var open by rememberSaveable { mutableStateOf(OpenTile.None) }
                fun toggle(tile: OpenTile) { open = if (open == tile) OpenTile.None else tile }
                val color = EntityColor.fromHex(form.color)

                // The avatar shows the colour and icon chosen below, next to the name it represents
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EntityAvatar(
                        name = form.name,
                        icon = iconFor(IconCategory.Group, form.icon),
                        color = color,
                        size = 64.dp,
                    )
                    FormTextField(
                        value = form.name,
                        onValueChange = { onEvent(GroupFormEvent.FormChanged(form.copy(name = it, nameError = null))) },
                        label = stringResource(Res.string.field_group_name),
                        errorRes = form.nameError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        onImeAction = { onEvent(GroupFormEvent.SaveClicked) },
                        modifier = Modifier.weight(1f),
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_appearance)) {
                    ColorFormTile(
                        selected = color,
                        expanded = open == OpenTile.Color,
                        onToggle = { toggle(OpenTile.Color) },
                        onSelect = { onEvent(GroupFormEvent.FormChanged(form.copy(color = it?.hex))) },
                    )
                    IconFormTile(
                        category = IconCategory.Group,
                        selectedKey = form.icon,
                        color = color,
                        expanded = open == OpenTile.Icon,
                        onToggle = { toggle(OpenTile.Icon) },
                        onSelect = { onEvent(GroupFormEvent.FormChanged(form.copy(icon = it))) },
                    )
                }

                FormSection(title = stringResource(Res.string.form_section_currency_period)) {
                    if (isEditing) {
                        FormTile(
                            label = stringResource(Res.string.field_currency),
                            note = stringResource(Res.string.currency_locked_note),
                        ) { FormTileValue(form.currency) }
                    } else {
                        ExpandableFormTile(
                            label = stringResource(Res.string.field_currency),
                            expanded = open == OpenTile.Currency,
                            onToggle = { toggle(OpenTile.Currency) },
                            expandedDescription = expandedStateText(open == OpenTile.Currency),
                            preview = { FormTileValue(form.currency) },
                        ) {
                            CurrencyChips(
                                selected = form.currency,
                                enabled = !state.isSubmitting,
                                onSelect = { onEvent(GroupFormEvent.FormChanged(form.copy(currency = it))) },
                            )
                            FormNote(stringResource(Res.string.new_group_currency_note), Modifier.padding(horizontal = 0.dp))
                        }
                    }
                    FormTile(
                        label = stringResource(Res.string.field_period_start_short),
                        note = stringResource(Res.string.period_start_day_note),
                    ) {
                        DayStepper(
                            day = form.periodStartDay,
                            enabled = !state.isSubmitting,
                            onChange = { onEvent(GroupFormEvent.FormChanged(form.copy(periodStartDay = it))) },
                        )
                    }
                    FormTile(
                        label = stringResource(Res.string.field_time_zone),
                        onClick = { onEvent(GroupFormEvent.TimeZoneClicked) },
                        enabled = !state.isSubmitting,
                    ) { FormTileValue(form.timezone) }
                }
                ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Which expandable tile of the form is open; one at a time keeps the form short. */
private enum class OpenTile { None, Color, Icon, Currency }

@Composable
private fun CurrencyChips(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupCurrencies.forEach { code ->
            FilterChip(
                selected = selected == code,
                onClick = { onSelect(code) },
                enabled = enabled,
                label = { Text(code) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
            )
        }
    }
}

/** "-  Day N  +" inside a tile, stopping at 1 and 28 so an invalid day cannot be chosen (D-29). */
@Composable
private fun DayStepper(day: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(day - 1) }, enabled = enabled && day > MIN_PERIOD_START_DAY) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.period_day_previous))
        }
        Text(
            text = stringResource(Res.string.period_day_value, day),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp),
        )
        IconButton(onClick = { onChange(day + 1) }, enabled = enabled && day < MAX_PERIOD_START_DAY) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.period_day_next))
        }
    }
}

@PreviewLightDark
@Composable
private fun GroupFormScreenPreview() {
    SobreioTheme { GroupFormScreen(state = GroupFormUiState(), isEditing = false, onEvent = {}) }
}
