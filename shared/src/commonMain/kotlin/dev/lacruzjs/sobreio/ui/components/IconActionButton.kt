package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A flat icon button with no text: [label] is its accessible name and the tooltip shown on long press. Shows a spinner
 * instead of the icon while [isLoading].
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    LabelTooltip(label = label, modifier = modifier) {
        IconButton(onClick = onClick, enabled = enabled && !isLoading, modifier = Modifier.size(56.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
            }
        }
    }
}
