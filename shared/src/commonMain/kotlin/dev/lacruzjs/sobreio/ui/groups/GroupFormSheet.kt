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
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.generated.resources.field_currency
import dev.lacruzjs.sobreio.generated.resources.field_group_name
import dev.lacruzjs.sobreio.generated.resources.new_group_currency_note
import dev.lacruzjs.sobreio.ui.components.AppearanceField
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet to create a group or, when [form] is editing one, to change it: name, currency (read-only once created),
 * colour and icon, the day the period starts and the time zone. State is hoisted: every change goes through [onChange]
 * and [onConfirm] saves.
 */
@Composable
internal fun GroupFormSheet(
    form: GroupFormState,
    onChange: (GroupFormState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean,
    submitError: AppError?,
) {
    val editing = form.isEditing
    FlatBottomSheet(
        title = stringResource(if (editing) Res.string.edit_group_title else Res.string.action_new_group),
        onDismiss = onDismiss,
    ) {
        FormTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it, nameError = null)) },
            label = stringResource(Res.string.field_group_name),
            errorRes = form.nameError,
            enabled = !isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = onConfirm,
        )
        AppearanceField(
            category = IconCategory.Group,
            name = form.name,
            icon = form.icon,
            color = EntityColor.fromHex(form.color),
            onChange = { icon, color -> onChange(form.copy(icon = icon, color = color?.hex)) },
            enabled = !isSubmitting,
        )
        if (editing) {
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
                        onClick = { onChange(form.copy(currency = code)) },
                        enabled = !isSubmitting,
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
            onChange = { onChange(form.copy(periodStartDay = it)) },
            enabled = !isSubmitting,
        )
        TimeZoneField(
            timezone = form.timezone,
            onSelect = { onChange(form.copy(timezone = it)) },
            enabled = !isSubmitting,
        )
        ErrorText(submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(if (editing) Res.string.action_save else Res.string.action_create),
            onClick = onConfirm,
            isLoading = isSubmitting,
        )
    }
}
