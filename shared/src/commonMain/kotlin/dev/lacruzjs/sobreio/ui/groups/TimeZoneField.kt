package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.time_zone_no_results
import dev.lacruzjs.sobreio.generated.resources.time_zone_search
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/** Every IANA time zone id the platform knows, sorted. Read once. */
private val AllTimeZones: List<String> by lazy { TimeZone.availableZoneIds.sorted() }

/** Read-style row that shows the chosen time zone id and opens [TimeZonePickerSheet] to change it. */
@Composable
internal fun TimeZoneField(timezone: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(Res.string.field_time_zone), style = MaterialTheme.typography.labelLarge)
        Text(
            text = timezone,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = enabled, role = Role.Button) { open = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
    if (open) {
        TimeZonePickerSheet(selected = timezone, onSelect = { onSelect(it); open = false }, onDismiss = { open = false })
    }
}

/** Sheet with a search field and the matching time zone ids; tapping one selects it. */
@Composable
internal fun TimeZonePickerSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(query) {
        val needle = query.trim().replace(' ', '_')
        if (needle.isEmpty()) AllTimeZones else AllTimeZones.filter { it.contains(needle, ignoreCase = true) }
    }
    FlatBottomSheet(title = stringResource(Res.string.field_time_zone), onDismiss = onDismiss) {
        FormTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(Res.string.time_zone_search),
        )
        if (matches.isEmpty()) {
            Text(
                text = stringResource(Res.string.time_zone_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(matches, key = { it }) { id ->
                    Text(
                        text = id,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (id == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onSelect(id) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}
