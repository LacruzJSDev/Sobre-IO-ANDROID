package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shows [label] in a small tooltip when [content] is long pressed. Used by the icon-only controls (swipe actions and the
 * Groups bottom row) so their names are still discoverable; screen readers get the label as the content description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelTooltip(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Box { content() }
    }
}
