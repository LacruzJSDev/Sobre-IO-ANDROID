package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkInk,
    onPrimary = DarkPaper,
    primaryContainer = DarkSoft,
    onPrimaryContainer = DarkInk,
    secondary = DarkMuted,
    onSecondary = DarkPaper,
    secondaryContainer = DarkSoft,
    onSecondaryContainer = DarkInk,
    tertiary = DarkInk,
    onTertiary = DarkPaper,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkPaper,
    onSurface = DarkInk,
    surfaceVariant = DarkSoft,
    onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = DarkPaper,
    surfaceContainerLow = DarkPaper,
    surfaceContainer = DarkPaper,
    surfaceContainerHigh = DarkSoft,
    surfaceContainerHighest = DarkSoft,
    outline = DarkMuted,
    outlineVariant = DarkLine,
)

private val LightColorScheme = lightColorScheme(
    primary = LightInk,
    onPrimary = LightPaper,
    primaryContainer = LightSoft,
    onPrimaryContainer = LightInk,
    secondary = LightMuted,
    onSecondary = LightPaper,
    secondaryContainer = LightSoft,
    onSecondaryContainer = LightInk,
    tertiary = LightInk,
    onTertiary = LightPaper,
    background = LightPaper,
    onBackground = LightInk,
    surface = LightPaper,
    onSurface = LightInk,
    surfaceVariant = LightSoft,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightPaper,
    surfaceContainerLow = LightPaper,
    surfaceContainer = LightPaper,
    surfaceContainerHigh = LightSoft,
    surfaceContainerHighest = LightSoft,
    outline = LightMuted,
    outlineVariant = LightLine,
)

@Composable
fun SobreioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = sobreioTypography(),
        content = content
    )
}
