package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.inter_medium
import dev.lacruzjs.sobreio.generated.resources.inter_regular
import dev.lacruzjs.sobreio.generated.resources.lexend_bold
import org.jetbrains.compose.resources.Font

// Lexend Bold for titles, with the brand alternates: single-bar I (ss01) and dotted zero
private fun TextStyle.brand(family: FontFamily) =
    copy(fontFamily = family, fontWeight = FontWeight.Bold, fontFeatureSettings = "ss01, zero")

// Inter for content, with tabular figures so amounts line up
private fun TextStyle.content(family: FontFamily) = copy(fontFamily = family, fontFeatureSettings = "tnum")

/** Composable because fonts are Compose resources, loaded through the composition. */
@Composable
fun sobreioTypography(): Typography {
    val lexend = FontFamily(Font(Res.font.lexend_bold, FontWeight.Bold))
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
    )
    return remember(lexend, inter) {
        val base = Typography()
        Typography(
            displayLarge = base.displayLarge.brand(lexend),
            displayMedium = base.displayMedium.brand(lexend),
            displaySmall = base.displaySmall.brand(lexend),
            headlineLarge = base.headlineLarge.brand(lexend),
            headlineMedium = base.headlineMedium.brand(lexend),
            headlineSmall = base.headlineSmall.brand(lexend),
            titleLarge = base.titleLarge.brand(lexend),
            titleMedium = base.titleMedium.content(inter),
            titleSmall = base.titleSmall.content(inter),
            bodyLarge = base.bodyLarge.content(inter),
            bodyMedium = base.bodyMedium.content(inter),
            bodySmall = base.bodySmall.content(inter),
            labelLarge = base.labelLarge.content(inter),
            labelMedium = base.labelMedium.content(inter),
            labelSmall = base.labelSmall.content(inter),
        )
    }
}
