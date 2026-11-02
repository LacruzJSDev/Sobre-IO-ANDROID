package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Paper,
    onPrimary = DarkPage,
    primaryContainer = DarkTileHigh,
    onPrimaryContainer = Paper,
    secondary = DarkMuted,
    onSecondary = DarkPage,
    secondaryContainer = DarkTileHigh,
    onSecondaryContainer = Paper,
    tertiary = Paper,
    onTertiary = DarkPage,
    background = DarkPage,
    onBackground = Paper,
    surface = DarkPage,
    onSurface = Paper,
    surfaceVariant = DarkTile,
    onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = DarkPage,
    surfaceContainerLow = DarkTile,
    surfaceContainer = DarkTile,
    surfaceContainerHigh = DarkTileHigh,
    surfaceContainerHighest = DarkTileHigh,
    outline = DarkMuted,
    outlineVariant = DarkLine,
)

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = LightTileHigh,
    onPrimaryContainer = Ink,
    secondary = LightMuted,
    onSecondary = Paper,
    secondaryContainer = LightTileHigh,
    onSecondaryContainer = Ink,
    tertiary = Ink,
    onTertiary = Paper,
    background = LightPage,
    onBackground = Ink,
    surface = LightPage,
    onSurface = Ink,
    surfaceVariant = LightTile,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightTile,
    surfaceContainerLow = LightTile,
    surfaceContainer = LightTile,
    surfaceContainerHigh = LightTileHigh,
    surfaceContainerHighest = LightTileHigh,
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
        typography = Typography,
        content = content
    )
}
