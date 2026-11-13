package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one button every variant is built on: full width, the app's corner radius, no elevation, and a spinner instead of
 * the text while [isLoading] (clicks are ignored then). Use [PrimaryButton], [SecondaryButton] or [DestructiveButton].
 */
@Composable
internal fun FlatButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // Keeps the colour while loading; a disabled button fades it
            disabledContainerColor = if (isLoading) containerColor else containerColor.copy(alpha = 0.4f),
            disabledContentColor = if (isLoading) contentColor else contentColor.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = contentColor)
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
