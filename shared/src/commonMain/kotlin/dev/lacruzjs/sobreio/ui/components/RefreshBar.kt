package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The thin ink line that says "bringing this up to date" while the screen keeps showing its figures and stays usable
 * (see `ui/loading/Revalidate.kt`). It is drawn OVER the bottom edge of the header, never in the layout flow, so
 * appearing and disappearing moves nothing. A refresh shorter than [showAfterMillis] never shows it, so cached answers
 * do not flicker. Ink, not a colour: the app is black and white.
 */
@Composable
fun RefreshBar(visible: Boolean, modifier: Modifier = Modifier, showAfterMillis: Long = 250) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(showAfterMillis)
            shown = true
        } else {
            shown = false
        }
    }
    AnimatedVisibility(visible = shown, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
            gapSize = 0.dp,
        )
    }
}
