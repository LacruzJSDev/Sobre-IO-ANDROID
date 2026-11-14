package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One family of corners. `medium` (10 dp) is the radius of the group avatar at its usual 40 dp size (a quarter of its
 * side), and buttons, text fields and swipe actions use it, so everything reads as the same square-ish shape.
 */
val SobreioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)
