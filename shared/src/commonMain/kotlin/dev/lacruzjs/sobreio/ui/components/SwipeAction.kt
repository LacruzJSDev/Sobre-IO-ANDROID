package dev.lacruzjs.sobreio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One button revealed by [SwipeActionsRow]: an [icon] on a [containerColor] background. [label] is its accessible name
 * and the tooltip shown on long press. Build them with the factories of SwipeActions.kt, so every action looks the same
 * everywhere.
 */
data class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit,
)
