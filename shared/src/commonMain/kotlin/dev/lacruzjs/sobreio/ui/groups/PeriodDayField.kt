package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.field_period_start_day
import dev.lacruzjs.sobreio.generated.resources.period_day_next
import dev.lacruzjs.sobreio.generated.resources.period_day_previous
import dev.lacruzjs.sobreio.generated.resources.period_day_value
import dev.lacruzjs.sobreio.generated.resources.period_start_day_note
import org.jetbrains.compose.resources.stringResource

/**
 * Stepper for the day of the month (1-28) a period starts on: "-", "Day N", "+". It stops at both ends, so an invalid
 * day cannot be picked. The note says that changing it only affects periods not created yet (D-29).
 */
@Composable
internal fun PeriodDayField(day: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(Res.string.field_period_start_day), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onChange(day - 1) },
                enabled = enabled && day > MIN_PERIOD_START_DAY,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.period_day_previous))
            }
            Text(
                text = stringResource(Res.string.period_day_value, day),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onChange(day + 1) },
                enabled = enabled && day < MAX_PERIOD_START_DAY,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.period_day_next))
            }
        }
        Text(
            text = stringResource(Res.string.period_start_day_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
