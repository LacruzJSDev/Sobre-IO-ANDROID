package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/*
 * Chart colours that are not part of the Material scheme. The variant follows the colour scheme in use (not the system
 * setting), so a dark preview of the theme gets the dark pair. The single balance line uses `primary` (the brand ink).
 */

private val ColorScheme.isDark: Boolean get() = background.luminance() < 0.5f

/** Money in (income, entries) in charts. */
val ColorScheme.chartIn: Color get() = if (isDark) DarkChartIn else LightChartIn

/** Money out (expenses, exits) in charts. */
val ColorScheme.chartOut: Color get() = if (isDark) DarkChartOut else LightChartOut
