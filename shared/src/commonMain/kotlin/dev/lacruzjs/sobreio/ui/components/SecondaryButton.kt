package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Alternative action next to a [PrimaryButton]: the soft grey surface with ink text, no outline. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    FlatButton(
        text = text,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
        isLoading = isLoading,
        enabled = enabled,
    )
}
