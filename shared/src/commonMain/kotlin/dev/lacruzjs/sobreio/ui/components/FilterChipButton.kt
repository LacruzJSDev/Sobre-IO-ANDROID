package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_clear_filter
import org.jetbrains.compose.resources.stringResource

/**
 * A filter of a list as a chip. Idle ([activeText] null) it is soft grey and shows [label] with a chevron; active it is
 * brand ink, shows [activeText] (with a small avatar in [activeLeading], for an account, envelope or member) and an X that
 * calls [onClear], so the filter goes away in one tap. Tapping anywhere else calls [onClick] to change it.
 */
@Composable
fun FilterChipButton(
    label: String,
    activeText: String?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    activeLeading: (@Composable () -> Unit)? = null,
) {
    val active = activeText != null
    val container = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val clearLabel = stringResource(Res.string.action_clear_filter)
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = if (active && activeLeading != null) 6.dp else 14.dp, end = if (active) 4.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (active && activeLeading != null) activeLeading()
        Text(
            text = activeText ?: label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (active) {
            // Its own tap target, so clearing never opens the sheet
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onClear)
                    .semantics { contentDescription = "$clearLabel: $label" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}
