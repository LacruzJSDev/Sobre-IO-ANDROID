package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Near-black grey of the hero card in dark mode: clearly lighter than the dark paper and than the soft surfaces. */
val DarkHero = Color(0xFF2D2D2A)

/** Background of the "below zero" chip: a paper-like light tone in both modes, so the negative colour reads on it. */
val HeroChipPaper = Color(0xFFFAFAF8)

/** Alpha of the labels drawn on the hero card. */
const val HeroLabelAlpha = 0.85f

/**
 * The colours of the hero card (the main figure of a tab). The card stays dark and prominent in both modes: the brand
 * ink in light mode and a near-black grey in dark mode (`primary` is near-white there, so it cannot be used).
 */
@Immutable
data class HeroColors(
    /** Background of the card. */
    val container: Color,
    /** Text and figures on the card: always white-ish, a negative figure is never recoloured. */
    val onContainer: Color,
    /** Background of the "below zero" chip. */
    val chipContainer: Color,
    /** Text of the chip: the negative money colour of the LIGHT palette, which is the one that contrasts with the chip. */
    val chipContent: Color,
    /** The negative money colour for text and lines drawn straight on the dark card (the dark palette's). */
    val negativeOnContainer: Color,
)

/** The hero colours for the light/dark and normal/colour-blind combination. */
fun heroColors(darkTheme: Boolean, colorBlindSafe: Boolean): HeroColors = HeroColors(
    container = if (darkTheme) DarkHero else LightInk,
    onContainer = if (darkTheme) DarkInk else LightPaper,
    chipContainer = HeroChipPaper,
    chipContent = moneyColors(darkTheme = false, colorBlindSafe = colorBlindSafe).negative,
    negativeOnContainer = moneyColors(darkTheme = true, colorBlindSafe = colorBlindSafe).negative,
)

/** The colour of the main figure of a hero card: always [HeroColors.onContainer], whatever its sign (see [showsBelowZeroChip]). */
fun heroFigureColor(colors: HeroColors, @Suppress("UNUSED_PARAMETER") amountCents: Long): Color = colors.onContainer

/** A negative main figure keeps its white colour and gets a small "below zero" chip next to it. */
fun showsBelowZeroChip(amountCents: Long): Boolean = amountCents < 0

val LocalHeroColors = staticCompositionLocalOf { heroColors(darkTheme = false, colorBlindSafe = false) }

/** Background of a `HeroCard`. */
val ColorScheme.heroContainer: Color
    @Composable @ReadOnlyComposable get() = LocalHeroColors.current.container

/** Text and figures on a `HeroCard`. */
val ColorScheme.onHeroContainer: Color
    @Composable @ReadOnlyComposable get() = LocalHeroColors.current.onContainer
