package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Role colours that are not part of the Material scheme. Leaving a group is muted orange (the Terracotta of the entity
 * palette, its lighter variant in dark mode): it is final enough to stand out from ink, but not red, which is kept for
 * what cannot be taken back.
 */
val ColorScheme.leaveAction: Color
    @Composable get() = if (isSystemInDarkTheme()) EntityColor.Terracotta.dark else EntityColor.Terracotta.light

val ColorScheme.onLeaveAction: Color
    @Composable get() = Color.White
