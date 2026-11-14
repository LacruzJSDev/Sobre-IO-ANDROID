package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.icon_none
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The icons of [category] in titled sections. The first cell is "no icon" (the avatar then shows the initial). The
 * selected cell is filled with [color] (ink when null) and shows a white icon; the others are soft grey. Scrolls on
 * its own, so give it a bounded height (for example `heightIn(max = ...)`) inside a bottom sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    category: IconCategory,
    selected: String?,
    color: EntityColor?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconCatalog.sections(category).forEachIndexed { index, section ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (index == 0) {
                        IconCell(
                            vector = null,
                            label = stringResource(Res.string.icon_none),
                            isSelected = selected == null,
                            color = color,
                        ) { onSelect(null) }
                    }
                    section.icons.forEach { option ->
                        IconCell(
                            vector = option.vector,
                            label = stringResource(option.label),
                            isSelected = selected == option.key,
                            color = color,
                        ) { onSelect(option.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconCell(vector: ImageVector?, label: String, isSelected: Boolean, color: EntityColor?, onClick: () -> Unit) {
    val background = if (isSelected) color.fill() else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isSelected) color.onFill() else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = vector ?: Icons.Filled.Block, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
    }
}

@PreviewLightDark
@Composable
private fun IconPickerPreview() {
    SobreioTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            IconPicker(category = IconCategory.Envelope, selected = "groceries", color = EntityColor.Green, onSelect = {})
        }
    }
}
