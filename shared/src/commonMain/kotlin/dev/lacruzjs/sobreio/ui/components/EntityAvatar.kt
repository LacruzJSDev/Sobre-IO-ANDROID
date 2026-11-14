package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme

/** The fill of an entity colour in the current theme, or the brand ink for no colour. */
@Composable
internal fun EntityColor?.fill(): Color = when {
    this == null -> MaterialTheme.colorScheme.primary
    isSystemInDarkTheme() -> dark
    else -> light
}

/** Content colour on top of [fill]: white on a palette colour, the paper colour on the ink. */
@Composable
internal fun EntityColor?.onFill(): Color = if (this == null) MaterialTheme.colorScheme.onPrimary else Color.White

/**
 * Square avatar of a group, account or envelope: filled with its [color] (brand ink when null) and showing its [icon]
 * in white, or the initial of [name] when there is no icon. Decorative: the name always appears next to it.
 */
@Composable
fun EntityAvatar(
    name: String,
    icon: ImageVector?,
    color: EntityColor?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val contentColor = color.onFill()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(color.fill()),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(size / 2))
        } else {
            Text(
                text = groupInitial(name),
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EntityAvatarPreview() {
    SobreioTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EntityAvatar("Casa", null, null)
                EntityAvatar("Casa", iconFor(IconCategory.Group, "home"), EntityColor.Blue)
                EntityAvatar("Ahorro", iconFor(IconCategory.Account, "savings"), EntityColor.Mustard)
                EntityAvatar("Comida", iconFor(IconCategory.Envelope, "groceries"), EntityColor.Terracotta)
                EntityAvatar("Coche", iconFor(IconCategory.Envelope, "car"), EntityColor.Violet)
                EntityAvatar("Banco", null, EntityColor.Wine, size = 56.dp)
            }
        }
    }
}
