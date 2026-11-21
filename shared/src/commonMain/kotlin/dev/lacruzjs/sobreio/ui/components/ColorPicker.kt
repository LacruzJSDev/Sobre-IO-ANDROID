package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.color_berry
import dev.lacruzjs.sobreio.generated.resources.color_blue
import dev.lacruzjs.sobreio.generated.resources.color_brown
import dev.lacruzjs.sobreio.generated.resources.color_coral
import dev.lacruzjs.sobreio.generated.resources.color_forest
import dev.lacruzjs.sobreio.generated.resources.color_graphite
import dev.lacruzjs.sobreio.generated.resources.color_green
import dev.lacruzjs.sobreio.generated.resources.color_indigo
import dev.lacruzjs.sobreio.generated.resources.color_mauve
import dev.lacruzjs.sobreio.generated.resources.color_moss
import dev.lacruzjs.sobreio.generated.resources.color_mustard
import dev.lacruzjs.sobreio.generated.resources.color_navy
import dev.lacruzjs.sobreio.generated.resources.color_none
import dev.lacruzjs.sobreio.generated.resources.color_ochre
import dev.lacruzjs.sobreio.generated.resources.color_olive
import dev.lacruzjs.sobreio.generated.resources.color_orange
import dev.lacruzjs.sobreio.generated.resources.color_plum
import dev.lacruzjs.sobreio.generated.resources.color_rust
import dev.lacruzjs.sobreio.generated.resources.color_sky
import dev.lacruzjs.sobreio.generated.resources.color_slate
import dev.lacruzjs.sobreio.generated.resources.color_taupe
import dev.lacruzjs.sobreio.generated.resources.color_teal
import dev.lacruzjs.sobreio.generated.resources.color_terracotta
import dev.lacruzjs.sobreio.generated.resources.color_violet
import dev.lacruzjs.sobreio.generated.resources.color_wine
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Accessible name of the colour. */
fun EntityColor.labelRes(): StringResource = when (this) {
    EntityColor.Navy -> Res.string.color_navy
    EntityColor.Blue -> Res.string.color_blue
    EntityColor.Sky -> Res.string.color_sky
    EntityColor.Teal -> Res.string.color_teal
    EntityColor.Forest -> Res.string.color_forest
    EntityColor.Green -> Res.string.color_green
    EntityColor.Moss -> Res.string.color_moss
    EntityColor.Olive -> Res.string.color_olive
    EntityColor.Mustard -> Res.string.color_mustard
    EntityColor.Ochre -> Res.string.color_ochre
    EntityColor.Orange -> Res.string.color_orange
    EntityColor.Rust -> Res.string.color_rust
    EntityColor.Terracotta -> Res.string.color_terracotta
    EntityColor.Coral -> Res.string.color_coral
    EntityColor.Wine -> Res.string.color_wine
    EntityColor.Berry -> Res.string.color_berry
    EntityColor.Mauve -> Res.string.color_mauve
    EntityColor.Plum -> Res.string.color_plum
    EntityColor.Violet -> Res.string.color_violet
    EntityColor.Indigo -> Res.string.color_indigo
    EntityColor.Slate -> Res.string.color_slate
    EntityColor.Graphite -> Res.string.color_graphite
    EntityColor.Brown -> Res.string.color_brown
    EntityColor.Taupe -> Res.string.color_taupe
}

/**
 * The 24 palette colours as swatches, preceded by "no colour" (brand ink). The selected one shows a check mark inside
 * a ring. Flat: no shadows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selected: EntityColor?,
    onSelect: (EntityColor?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Swatch(color = null, label = stringResource(Res.string.color_none), isSelected = selected == null) { onSelect(null) }
        EntityColor.entries.forEach { color ->
            Swatch(color = color, label = stringResource(color.labelRes()), isSelected = selected == color) { onSelect(color) }
        }
    }
}

@Composable
private fun Swatch(color: EntityColor?, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (isSelected) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label }
            .padding(5.dp)
            .clip(CircleShape)
            .background(color.fill()),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = color.onFill(), modifier = Modifier.size(20.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorPickerPreview() {
    SobreioTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            ColorPicker(selected = EntityColor.Teal, onSelect = {})
        }
    }
}
