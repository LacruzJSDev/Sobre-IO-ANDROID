package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_close
import dev.lacruzjs.sobreio.generated.resources.action_save
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen form (create or edit a group, account or envelope): a header with a close X on the left (leave without
 * saving), the [title] and a "Save" action on the right (disabled with a spinner while [isSaving]), and the fields in a
 * scrolling column that rises above the keyboard. Flat, on the paper background, no dividers. System back does what the
 * X does, which the Route wires.
 */
@Composable
fun FormScaffold(
    title: String,
    onClose: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // A tap outside the fields closes the keyboard
        Column(modifier = Modifier.fillMaxSize().clearFocusOnTap()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, enabled = !isSaving) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                TextButton(onClick = onSave, enabled = !isSaving) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.action_save), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}
