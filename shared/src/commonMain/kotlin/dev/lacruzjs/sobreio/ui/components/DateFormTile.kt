package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_cancel
import dev.lacruzjs.sobreio.generated.resources.action_clear_date
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.ui.format.formatDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * A [FormTile] that shows a date and opens a flat Material 3 date picker dialog when tapped. [date] null shows [emptyText]
 * (for an optional date); [maxDate] is the last day the picker allows (null: any); [onClear] adds a clear button while a date is set. The picker works on UTC midnights, so the
 * day chosen is the day shown whatever the device's time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFormTile(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String = "",
    note: String? = null,
    enabled: Boolean = true,
    onClear: (() -> Unit)? = null,
    maxDate: LocalDate? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val languageTag = Locale.current.toLanguageTag()
    FormTile(
        label = label,
        modifier = modifier,
        note = note,
        onClick = { open = true },
        enabled = enabled,
    ) {
        FormTileValue(text = date?.let { formatDate(it, languageTag) } ?: emptyText)
        if (onClear != null && date != null) {
            IconButton(onClick = onClear, enabled = enabled) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_clear_date))
            }
        }
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toPickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    maxDate == null || utcTimeMillis <= maxDate.toPickerMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateSelected(it.toPickerDate()) }
                        open = false
                    },
                ) { Text(stringResource(Res.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(Res.string.action_cancel)) } },
            // Flat, like every sheet and dialog of the app
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background))
        }
    }
}

internal fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal fun Long.toPickerDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
