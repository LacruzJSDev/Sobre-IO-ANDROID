package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.color_none
import dev.lacruzjs.sobreio.generated.resources.field_color
import dev.lacruzjs.sobreio.generated.resources.field_icon
import dev.lacruzjs.sobreio.generated.resources.icon_none
import dev.lacruzjs.sobreio.generated.resources.state_collapsed
import dev.lacruzjs.sobreio.generated.resources.state_expanded
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/*
 * The "Appearance" tiles every form shares (group, account, envelope): Colour and Icon as expandable tiles. The form
 * keeps which tile is open (one at a time) and passes it in.
 */

/** Expanded or collapsed, as the accessibility state of an [ExpandableFormTile]. */
@Composable
fun expandedStateText(expanded: Boolean): String =
    stringResource(if (expanded) Res.string.state_expanded else Res.string.state_collapsed)

/** The chosen colour as a small dot in a tile header; the ink for no colour. */
@Composable
fun ColorDot(color: EntityColor?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(20.dp).clip(CircleShape).background(color.fill()))
}

/** "Colour": the current choice as its name and a dot; the palette opens under it. */
@Composable
fun ColorFormTile(
    selected: EntityColor?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (EntityColor?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandableFormTile(
        label = stringResource(Res.string.field_color),
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        expandedDescription = expandedStateText(expanded),
        preview = {
            FormTileValue(stringResource(selected?.labelRes() ?: Res.string.color_none))
            ColorDot(selected)
        },
    ) {
        ColorPicker(selected = selected, onSelect = onSelect)
    }
}

/** "Icon" of [category]: the current choice as its name and glyph; the grid opens under it on the paper colour. */
@Composable
fun IconFormTile(
    category: IconCategory,
    selectedKey: String?,
    color: EntityColor?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandableFormTile(
        label = stringResource(Res.string.field_icon),
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        expandedDescription = expandedStateText(expanded),
        preview = {
            val option = IconCatalog.iconFor(category, selectedKey)
            if (option == null) {
                FormTileValue(stringResource(Res.string.icon_none))
            } else {
                FormTileValue(stringResource(option.label))
                Icon(option.vector, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        },
    ) {
        IconPicker(
            category = category,
            selected = selectedKey,
            color = color,
            onSelect = onSelect,
            cellColor = MaterialTheme.colorScheme.background,
        )
    }
}
