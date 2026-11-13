package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The red button for actions that end something (sign out, delete, leave): filled red, flat. */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    FlatButton(
        text = text,
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = modifier,
        isLoading = isLoading,
        enabled = enabled,
    )
}
