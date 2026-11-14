package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_done
import dev.lacruzjs.sobreio.generated.resources.appearance_field_label
import dev.lacruzjs.sobreio.generated.resources.appearance_title
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Compact row for create/edit sheets: previews the entity's avatar and opens an [AppearanceSheet] to choose its
 * colour and icon, so the sheet that holds it stays short. State is hoisted: [onChange] gets the new (icon, colour).
 */
@Composable
fun AppearanceField(
    category: IconCategory,
    name: String,
    icon: String?,
    color: EntityColor?,
    onChange: (icon: String?, color: EntityColor?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, role = Role.Button) { open = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntityAvatar(name = name, icon = iconFor(category, icon), color = color)
        Text(text = stringResource(Res.string.appearance_field_label), style = MaterialTheme.typography.bodyLarge)
    }
    if (open) {
        AppearanceSheet(
            category = category,
            name = name,
            icon = icon,
            color = color,
            onChange = onChange,
            onDismiss = { open = false },
        )
    }
}

/** Bottom sheet with the [ColorPicker] on top and the [IconPicker] below it; changes apply at once, "Done" closes it. */
@Composable
fun AppearanceSheet(
    category: IconCategory,
    name: String,
    icon: String?,
    color: EntityColor?,
    onChange: (icon: String?, color: EntityColor?) -> Unit,
    onDismiss: () -> Unit,
) {
    FlatBottomSheet(title = stringResource(Res.string.appearance_title), onDismiss = onDismiss) {
        EntityAvatar(name = name, icon = iconFor(category, icon), color = color, size = 56.dp)
        ColorPicker(selected = color, onSelect = { onChange(icon, it) })
        IconPicker(
            category = category,
            selected = icon,
            color = color,
            onSelect = { onChange(it, color) },
            modifier = Modifier.heightIn(max = 300.dp),
        )
        PrimaryButton(text = stringResource(Res.string.action_done), onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@PreviewLightDark
@Composable
private fun AppearanceFieldPreview() {
    SobreioTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            AppearanceField(IconCategory.Account, "Santander", "bank", EntityColor.Wine, { _, _ -> })
        }
    }
}
