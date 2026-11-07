package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.ui.error.messageRes

/** The general (not field-level) error of a form; draws nothing when [error] is null. */
@Composable
fun ErrorText(error: AppError?, modifier: Modifier = Modifier) {
    if (error != null) {
        Text(
            text = stringResource(error.messageRes()),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
