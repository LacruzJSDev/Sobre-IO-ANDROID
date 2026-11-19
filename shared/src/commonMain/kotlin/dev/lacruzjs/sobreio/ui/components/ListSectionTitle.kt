package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The small muted title that splits a list into sections (In envelopes / Outside, Monthly spending / Bills, a day of the
 * Movements list…). Lists use it so every section looks the same.
 */
@Composable
fun ListSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
}
