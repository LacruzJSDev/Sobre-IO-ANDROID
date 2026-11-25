package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.tag_color_ice
import dev.lacruzjs.sobreio.generated.resources.tag_color_olive
import dev.lacruzjs.sobreio.generated.resources.tag_color_orchid
import dev.lacruzjs.sobreio.generated.resources.tag_color_apricot
import dev.lacruzjs.sobreio.generated.resources.tag_color_aqua
import dev.lacruzjs.sobreio.generated.resources.tag_color_blush
import dev.lacruzjs.sobreio.generated.resources.tag_color_butter
import dev.lacruzjs.sobreio.generated.resources.tag_color_lavender
import dev.lacruzjs.sobreio.generated.resources.tag_color_lilac
import dev.lacruzjs.sobreio.generated.resources.tag_color_mint
import dev.lacruzjs.sobreio.generated.resources.tag_color_pistachio
import dev.lacruzjs.sobreio.generated.resources.tag_color_rose
import dev.lacruzjs.sobreio.generated.resources.tag_color_sky
import dev.lacruzjs.sobreio.generated.resources.tag_color_stone
import dev.lacruzjs.sobreio.ui.theme.TagColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Accessible name of the colour. */
fun TagColor.labelRes(): StringResource = when (this) {
    TagColor.Rose -> Res.string.tag_color_rose
    TagColor.Blush -> Res.string.tag_color_blush
    TagColor.Apricot -> Res.string.tag_color_apricot
    TagColor.Butter -> Res.string.tag_color_butter
    TagColor.Olive -> Res.string.tag_color_olive
    TagColor.Pistachio -> Res.string.tag_color_pistachio
    TagColor.Mint -> Res.string.tag_color_mint
    TagColor.Aqua -> Res.string.tag_color_aqua
    TagColor.Ice -> Res.string.tag_color_ice
    TagColor.Sky -> Res.string.tag_color_sky
    TagColor.Lavender -> Res.string.tag_color_lavender
    TagColor.Lilac -> Res.string.tag_color_lilac
    TagColor.Orchid -> Res.string.tag_color_orchid
    TagColor.Stone -> Res.string.tag_color_stone
}

/** The colour of a tag's dot in the current theme; a tag with no (or an unknown) colour gets a muted grey. */
@Composable
fun tagDotColor(hex: String?): Color {
    val color = TagColor.fromHex(hex)
    return when {
        color == null -> MaterialTheme.colorScheme.outline
        isSystemInDarkTheme() -> color.dark
        else -> color.light
    }
}

/** The small round colour mark that stands for a tag next to its name. */
@Composable
fun TagDot(hex: String?, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(modifier = modifier.size(size).clip(CircleShape).background(tagDotColor(hex)))
}

/** The tag palette as a row of round swatches; the selected one shows a check inside a ring. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagColorPicker(
    selected: TagColor?,
    onSelect: (TagColor) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TagColor.entries.forEach { color ->
            val isSelected = selected == color
            val label = stringResource(color.labelRes())
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                    .selectable(selected = isSelected, enabled = enabled, onClick = { onSelect(color) }, role = Role.RadioButton)
                    .semantics { contentDescription = label }
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(tagDotColor(color.hex)),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
