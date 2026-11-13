package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.coming_soon
import org.jetbrains.compose.resources.stringResource

/** Stand-in for the creation sheet of a feature that does not exist yet; [title] says what would be created. */
@Composable
fun ComingSoonSheet(title: String, onDismiss: () -> Unit) {
    FlatBottomSheet(title = title, onDismiss = onDismiss) {
        Text(
            text = stringResource(Res.string.coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
