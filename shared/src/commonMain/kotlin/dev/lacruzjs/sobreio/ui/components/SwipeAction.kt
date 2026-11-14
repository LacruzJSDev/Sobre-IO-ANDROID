package dev.lacruzjs.sobreio.ui.components

import androidx.compose.ui.graphics.Color

/** One button revealed by [SwipeActionsRow]: [label] on a [containerColor] background. */
data class SwipeAction(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit,
)
