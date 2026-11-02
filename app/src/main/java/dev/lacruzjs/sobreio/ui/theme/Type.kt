package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.lacruzjs.sobreio.R

val Lexend = FontFamily(Font(R.font.lexend_bold, FontWeight.Bold))

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)

// Lexend Bold for titles, with the brand alternates: single-bar I (ss01) and dotted zero
private fun TextStyle.brand() =
    copy(fontFamily = Lexend, fontWeight = FontWeight.Bold, fontFeatureSettings = "ss01, zero")

// Inter for content, with tabular figures so amounts line up
private fun TextStyle.content() = copy(fontFamily = Inter, fontFeatureSettings = "tnum")

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.brand(),
    displayMedium = base.displayMedium.brand(),
    displaySmall = base.displaySmall.brand(),
    headlineLarge = base.headlineLarge.brand(),
    headlineMedium = base.headlineMedium.brand(),
    headlineSmall = base.headlineSmall.brand(),
    titleLarge = base.titleLarge.brand(),
    titleMedium = base.titleMedium.content(),
    titleSmall = base.titleSmall.content(),
    bodyLarge = base.bodyLarge.content(),
    bodyMedium = base.bodyMedium.content(),
    bodySmall = base.bodySmall.content(),
    labelLarge = base.labelLarge.content(),
    labelMedium = base.labelMedium.content(),
    labelSmall = base.labelSmall.content(),
)
