package dev.lacruzjs.sobreio.ui.movementform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.filter_tags
import dev.lacruzjs.sobreio.generated.resources.movement_tags_more
import dev.lacruzjs.sobreio.generated.resources.tag_picker_create
import dev.lacruzjs.sobreio.generated.resources.tag_picker_empty
import dev.lacruzjs.sobreio.generated.resources.tag_picker_none
import dev.lacruzjs.sobreio.generated.resources.tag_picker_search
import dev.lacruzjs.sobreio.generated.resources.tag_picker_suggestion
import dev.lacruzjs.sobreio.generated.resources.tag_remove
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ExpandableFormTile
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.SearchField
import dev.lacruzjs.sobreio.ui.components.TagDot
import org.jetbrains.compose.resources.stringResource

private val ListMaxHeight = 240.dp

/**
 * The tags of a movement: an expandable tile like [dev.lacruzjs.sobreio.ui.components.EntitySelect] but with several
 * choices. Closed, its header shows the chosen tags as small chips (two, then "+N"). Open, it has a search field (case and accent insensitive), a «Did you mean…?» line when a tag is the same or a
 * close one, a first «Create "text"» row when the text names no tag yet, and the live tags with their colour dot and a
 * check. The chosen tags show as removable chips at the bottom of the open body.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagPicker(
    state: MovementFormUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEvent: (MovementFormEvent) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val options = state.tagPicker
    ExpandableFormTile(
        label = stringResource(Res.string.filter_tags),
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        preview = {
            if (state.selectedTags.isEmpty()) {
                Text(
                    text = stringResource(Res.string.tag_picker_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.selectedTags.take(PREVIEW_TAGS).forEach { tag ->
                    PreviewTagChip(tag, modifier = Modifier.weight(1f, fill = false))
                }
                val more = state.selectedTags.size - PREVIEW_TAGS
                if (more > 0) {
                    Text(
                        text = stringResource(Res.string.movement_tags_more, more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        SearchField(
            value = state.tagQuery,
            onValueChange = { onEvent(MovementFormEvent.TagQueryChanged(it)) },
            placeholder = stringResource(Res.string.tag_picker_search),
            containerColor = MaterialTheme.colorScheme.background,
            enabled = enabled,
            onSearch = { if (enabled) options.createName?.let { onEvent(MovementFormEvent.TagCreateClicked) } },
        )
        state.tagError?.let { error ->
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        options.suggestion?.let { tag ->
            Text(
                text = stringResource(Res.string.tag_picker_suggestion, tag.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(enabled = enabled, role = Role.Button) {
                        if (tag.id !in state.selectedTagIds) onEvent(MovementFormEvent.TagToggled(tag.id))
                        onEvent(MovementFormEvent.TagQueryChanged(""))
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        if (options.createName != null) {
            CreateRow(
                name = options.createName,
                enabled = enabled && !state.isCreatingTag,
                onClick = { onEvent(MovementFormEvent.TagCreateClicked) },
            )
        }
        if (state.tags.isEmpty() && state.tagQuery.isBlank()) {
            FormNote(stringResource(Res.string.tag_picker_empty))
        } else {
            // Bounded height: it scrolls on its own inside the scrolling form
            LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
                items(options.rows, key = { it.id }) { tag ->
                    TagRow(
                        tag = tag,
                        selected = tag.id in state.selectedTagIds,
                        enabled = enabled,
                        onClick = { onEvent(MovementFormEvent.TagToggled(tag.id)) },
                    )
                }
            }
        }
        ErrorText(state.tagFailure, modifier = Modifier.fillMaxWidth())
        if (state.selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.selectedTags.forEach { tag ->
                    TagChip(tag = tag, enabled = enabled, onRemove = { onEvent(MovementFormEvent.TagToggled(tag.id)) })
                }
            }
        }
    }
}

/** How many chosen tags the closed header names before the "+N". */
private const val PREVIEW_TAGS = 2

/** A chosen tag in the closed header: colour dot and name on a paper pill. */
@Composable
private fun PreviewTagChip(tag: Tag, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TagDot(hex = tag.color, size = 10.dp)
        Text(text = tag.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CreateRow(name: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(Res.string.tag_picker_create, name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagRow(tag: Tag, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TagDot(hex = tag.color, size = 14.dp)
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A chosen tag as a small chip: colour dot, name and an X that removes it from the movement. */
@Composable
private fun TagChip(tag: Tag, enabled: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TagDot(hex = tag.color, size = 12.dp)
        Text(text = tag.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.tag_remove, tag.name),
            modifier = Modifier.size(16.dp),
        )
    }
}
