package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_cancel
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.ui.components.toPickerDate
import dev.lacruzjs.sobreio.ui.components.toPickerMillis
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** A flat Material 3 date range picker in a dialog, like the single date one of the forms. Works on UTC midnights. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangeDialog(
    from: LocalDate?,
    to: LocalDate?,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = from?.toPickerMillis(),
        initialSelectedEndDateMillis = to?.toPickerMillis(),
    )
    val start = state.selectedStartDateMillis
    val end = state.selectedEndDateMillis
    val colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null && end != null,
                onClick = {
                    if (start != null && end != null) onConfirm(start.toPickerDate(), end.toPickerDate())
                },
            ) { Text(stringResource(Res.string.action_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
        // Flat, like every sheet and dialog of the app
        tonalElevation = 0.dp,
        colors = colors,
    ) {
        DateRangePicker(state = state, colors = colors, modifier = Modifier.height(520.dp))
    }
}
