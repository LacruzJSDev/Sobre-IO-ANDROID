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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme

/**
 * The one building block of every expand and collapse: a soft grey surface whose header (the [label], a [preview] of the
 * current choice at the end and a chevron that turns) and body are the same surface, no gap, no border, no second card.
 * Open, a thin divider and then the [content] appear under the header. The caller keeps [expanded]. With [enabled] false
 * the header only shows its value (no chevron, no tap). [flushContent] drops the horizontal padding of the body, for lists
 * of rows that pad themselves.
 */
@Composable
internal fun ExpandableSurface(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expandedDescription: String? = null,
    flushContent: Boolean = false,
    preview: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    val stateText = expandedDescription ?: expandedStateText(expanded)
    val dismissKeyboard = rememberDismissKeyboard()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(LocalTileShape.current ?: MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled, role = Role.Button) { dismissKeyboard(); onToggle() }
                .semantics { stateDescription = stateText }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                content = preview,
            )
            if (enabled) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).rotate(arrowRotation),
                )
            } else {
                Spacer(Modifier.size(4.dp))
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (flushContent) 0.dp else 16.dp,
                            end = if (flushContent) 0.dp else 16.dp,
                            top = if (flushContent) 4.dp else 12.dp,
                            bottom = if (flushContent) 8.dp else 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ExpandableSurfacePreview() {
    SobreioTheme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpandableSurface(label = "Closed", expanded = false, onToggle = {}) { Text("Hidden") }
            ExpandableSurface(
                label = "Open",
                expanded = true,
                onToggle = {},
                preview = { Text("Preview", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            ) { Text("Body of the surface") }
        }
    }
}
