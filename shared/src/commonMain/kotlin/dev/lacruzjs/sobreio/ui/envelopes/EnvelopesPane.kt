package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Envelopes tab: summary, sections, undo snackbar and the create, edit and assign sheets. It lives
 * inside the group shell, so the snackbar sits above the navigation bar. The "+" is in the shell's header.
 */
@Composable
fun EnvelopesPane(
    state: EnvelopesUiState,
    onEvent: (EnvelopesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The list is the only part that scrolls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && state.unassigned == null -> EmptyState(
                    message = stringResource(state.error.messageRes()),
                ) {
                    TextButton(onClick = { onEvent(EnvelopesEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                else -> EnvelopeList(state, onEvent)
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage,
            onUndo = { onEvent(EnvelopesEvent.UndoClicked) },
            onDismiss = { onEvent(EnvelopesEvent.UndoExpired) },
        )
    }

    when (state.sheet) {
        EnvelopesSheet.None -> Unit
        EnvelopesSheet.Create, EnvelopesSheet.Edit -> EnvelopeFormSheet(state, onEvent)
        EnvelopesSheet.Assign -> AssignSheet(state, onEvent)
    }
}

private val previewUnassigned = Envelope("0", "g", "Sin asignar", EnvelopeKind.System, 0, 25000)
private val previewEnvelopes = listOf(
    Envelope("1", "g", "Comida", EnvelopeKind.Spending, 20000, 12050),
    Envelope("2", "g", "Vacaciones", EnvelopeKind.Saving, 0, 80000),
)

@PreviewLightDark
@Composable
private fun EnvelopesPanePreview() {
    SobreioTheme {
        EnvelopesPane(
            state = EnvelopesUiState(
                envelopes = previewEnvelopes,
                unassigned = previewUnassigned,
                totalCents = 117050,
                unassignedCents = 25000,
                isLoading = false,
                canEdit = true,
            ),
            onEvent = {},
        )
    }
}
