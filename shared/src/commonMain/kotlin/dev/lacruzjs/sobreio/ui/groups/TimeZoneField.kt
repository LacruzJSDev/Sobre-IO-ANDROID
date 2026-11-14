package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.time_zone_no_results
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

/** Every IANA time zone id the platform knows, sorted. Read once. */
private val AllTimeZones: List<String> by lazy { TimeZone.availableZoneIds.sorted() }

private const val MAX_MATCHES = 50

/** The ids that contain [query] (spaces count as underscores), at most [MAX_MATCHES]; the first ones for a blank query. */
internal fun matchingTimeZones(query: String, all: List<String> = AllTimeZones): List<String> {
    val needle = query.trim().replace(' ', '_')
    return (if (needle.isEmpty()) all else all.filter { it.contains(needle, ignoreCase = true) }).take(MAX_MATCHES)
}

/**
 * A select with search inside the form, with no popup: an editable field and, while it has focus, the matching ids
 * listed right above it (the keyboard would hide what is under it) (about 6 rows visible, scrollable, at most 50). Focusing clears the text and shows the current
 * zone as placeholder; typing filters at once. Choosing one sets it and drops the focus (the keyboard hides); leaving the
 * field without choosing keeps the previous value, so the stored one is always a valid zone id.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimeZoneField(timezone: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var focused by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val bringIntoView = remember { BringIntoViewRequester() }
    // The current zone comes first when nothing is typed, so it is the one in view
    val matches = remember(query, timezone) {
        val found = matchingTimeZones(query)
        if (query.isBlank()) (listOf(timezone) + found.filter { it != timezone }).take(MAX_MATCHES) else found
    }

    // Keep the field and its suggestions above the keyboard while it is being used
    // Again each time the keyboard changes size, so the field and its suggestions end up above it
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, matches.size, imeBottom) { if (focused) bringIntoView.bringIntoView() }

    Column(modifier = modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)) {
        // Above the field while focused: the keyboard covers whatever is under it
        if (focused) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = RowHeight * 6)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (matches.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.time_zone_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                }
                matches.forEach { id ->
                    Text(
                        text = id,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (id == timezone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = RowHeight)
                            .clickable(role = Role.Button) {
                                onSelect(id)
                                // On iOS clearing the focus alone leaves the keyboard up
                                keyboard?.hide()
                                focusManager.clearFocus(force = true)
                            }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                    )
                }
            }
        }
        OutlinedTextField(
            value = if (focused) query else timezone,
            onValueChange = { query = it },
            label = { Text(stringResource(Res.string.field_time_zone)) },
            placeholder = { Text(timezone) },
            singleLine = true,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    // Whatever was typed and not chosen is dropped
                    query = ""
                },
        )
    }
}

private val RowHeight = 48.dp
