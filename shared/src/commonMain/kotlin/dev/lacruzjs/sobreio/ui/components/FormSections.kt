package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/*
 * Building blocks of the full-screen forms. A form is a few sections; each section has a small muted title and a stack of
 * soft tiles. A tile is either a value (label on the left, value or control on the right) or an expandable one whose
 * content (colour palette, icon grid) opens under it. Flat: soft background, rounded corners, spacing, no dividers.
 */

/** A titled group of tiles. */
@Composable
fun FormSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        content()
    }
}

/**
 * A tile with [label] on the left and [trailing] (a value, a stepper…) on the right. Clickable when [onClick] is given,
 * for a value chosen on another screen (time zone). [note] is a short explanation under the tile.
 */
@Composable
fun FormTile(
    label: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    else Modifier,
                )
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailing()
        }
        if (note != null) FormNote(note)
    }
}

/** The value shown at the right of a [FormTile]. */
@Composable
fun FormTileValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(end = 8.dp),
    )
}

/**
 * A tile that opens its [content] under the header when tapped. The header shows [label], a [preview] of the current
 * choice and an arrow that turns when open. The caller keeps [expanded], so a form can keep only one open at a time.
 */
@Composable
fun ExpandableFormTile(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    expandedDescription: String? = null,
    preview: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { if (expandedDescription != null) stateDescription = expandedDescription }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            preview()
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp).rotate(arrowRotation),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Short muted explanation under a tile or a field. */
@Composable
fun FormNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
