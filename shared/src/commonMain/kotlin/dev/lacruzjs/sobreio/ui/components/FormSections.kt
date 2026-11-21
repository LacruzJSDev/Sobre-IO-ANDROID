package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Building blocks of the full-screen forms. A form is a few sections; each section has a small muted title and a stack of
 * soft tiles. A tile is either a value (label on the left, value or control on the right) or an expandable one whose
 * content (colour palette, icon grid) opens under it. Flat: soft background, rounded corners, spacing, no dividers.
 */

/** The shape a tile takes inside a segmented [FormSection] (small inner corners); null outside one. */
val LocalTileShape = compositionLocalOf<Shape?> { null }

/** The shape of a soft tile: the small inner corner of a segmented section, the medium corner anywhere else. */
@Composable
fun tileShape(): Shape = LocalTileShape.current ?: MaterialTheme.shapes.medium

private val SegmentGap = 2.dp
private val InnerTileShape = RoundedCornerShape(4.dp)

/**
 * A titled group of tiles. [action] sits at the right of the title: in a read-only detail, the icon that opens the
 * screen where that section's items are all listed and managed (never a "See all" button under the section).
 *
 * A [segmented] section (the default) is one block: its tiles are [SegmentGap] apart, the outer corners of the first and
 * last are the large shape and the inner ones are small ([LocalTileShape]). Captions and error texts between tiles are
 * transparent, so they just sit in the gaps. A section whose content is not made of tiles (a figure, plain rows, a
 * chart) passes `segmented = false` and keeps the plain spacing and no clip. The space between sections is the 16 dp of
 * the scaffold plus the 8 dp on top of each section.
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    segmented: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 4.dp, bottom = 2.dp),
            )
            action?.invoke()
        }
        if (segmented) {
            CompositionLocalProvider(LocalTileShape provides InnerTileShape) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                    verticalArrangement = Arrangement.spacedBy(SegmentGap),
                    content = content,
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(tileShape())
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

/**
 * A read only tile in the look of [FormTile] with a small [title], a main line and an optional muted [secondary] line
 * under it, for a short status that needs two lines (the reservation of a funded commitment).
 */
@Composable
fun FormInfoTile(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(tileShape())
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
        if (secondary != null) {
            Text(text = secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A tile for a short, fixed choice that is better seen than hidden behind an arrow (the four envelope types): the
 * [label] on top and the options as [FormChip]s under it, wrapping to more lines when they do not fit. [error] is a
 * validation or database message shown under the tile in the error colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFormTile(
    label: String,
    modifier: Modifier = Modifier,
    error: StringResource? = null,
    chips: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(tileShape())
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
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
 * A tile that opens its [content] inside the same surface, under a divider, when tapped (an [ExpandableSurface]). The
 * header shows [label], a [preview] of the current choice and an arrow that turns when open. The caller keeps [expanded], so a form can keep only one open at a time.
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
    ExpandableSurface(
        label = label,
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        expandedDescription = expandedDescription,
        preview = preview,
        content = content,
    )
}

/** Short muted explanation under a tile or a field. */
@Composable
fun FormNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}
