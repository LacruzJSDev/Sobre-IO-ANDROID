package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A flat icon button with no text: [label] is its accessible name and the tooltip shown on long press. Shows a spinner
 * instead of the icon while [isLoading]. A [badge] above 0 draws that count in a small circle on the icon, the same badge
 * as the Group tab's (say what it counts in [label], since the number alone is not read out).
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
    /** 28 dp for header and toolbar actions; 24 dp sits next to a section title, matching the "+" of `AddIconButton`. */
    iconSize: Dp = 28.dp,
    badge: Int = 0,
) {
    LabelTooltip(label = label, modifier = modifier) {
        IconButton(onClick = onClick, enabled = enabled && !isLoading, modifier = Modifier.size(56.dp)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = tint)
            } else if (badge > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { Text(badge.toString()) }
                    },
                ) { Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(iconSize)) }
            } else {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(iconSize))
            }
        }
    }
}
