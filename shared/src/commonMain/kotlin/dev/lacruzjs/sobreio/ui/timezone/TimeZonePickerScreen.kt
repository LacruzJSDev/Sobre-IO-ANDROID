package dev.lacruzjs.sobreio.ui.timezone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.time_zone_current
import dev.lacruzjs.sobreio.generated.resources.time_zone_no_results
import dev.lacruzjs.sobreio.generated.resources.time_zone_search
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/** Every IANA time zone id the platform knows, sorted. Read once. */
private val AllTimeZones: List<String> by lazy { TimeZone.availableZoneIds.sorted() }

/**
 * The ids to list: the [current] one first, then the rest of [all] that contain [query] (case-insensitive, spaces match
 * underscores). A blank query lists everything.
 */
internal fun filterTimeZones(query: String, current: String, all: List<String> = AllTimeZones): List<String> {
    val needle = query.trim().replace(' ', '_')
    val matching = if (needle.isEmpty()) all else all.filter { it.contains(needle, ignoreCase = true) }
    return if (current in matching) listOf(current) + matching.filter { it != current } else matching
}

/** Full screen to choose a time zone: search field on top (focused on arrival) and the matching ids below. */
@Composable
fun TimeZonePickerRoute(current: String, onBack: () -> Unit, onPicked: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val zones = remember(query, current) { filterTimeZones(query, current) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(Res.string.field_time_zone), onBack = onBack)
            FormTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(Res.string.time_zone_search),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .focusRequester(focus),
            )
            if (zones.isEmpty()) {
                Text(
                    text = stringResource(Res.string.time_zone_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            ) {
                items(zones, key = { it }) { id ->
                    val isCurrent = id == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            // One tap selects, even with the keyboard up: the keyboard hides in the same click
                            .clickable(role = Role.Button) {
                                keyboard?.hide()
                                onPicked(id)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = id,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(Res.string.time_zone_current),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
