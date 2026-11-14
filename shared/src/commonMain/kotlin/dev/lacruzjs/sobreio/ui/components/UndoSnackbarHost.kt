package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_undo
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.getString

/**
 * Bottom snackbar with an "Undo" action for [message]; null shows nothing. [onUndo] runs on the action, [onDismiss]
 * when it times out or is swiped away. Flat: ink background, paper text, no shadow. Place it in the layout where it
 * should appear (above the bottom buttons, for example).
 */
@Composable
fun UndoSnackbarHost(
    message: UndoMessage?,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(message?.key) {
        if (message == null) {
            hostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val result = hostState.showSnackbar(
            message = getString(message.message, message.argument),
            actionLabel = getString(Res.string.action_undo),
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> currentOnUndo()
            SnackbarResult.Dismissed -> currentOnDismiss()
        }
    }

    FlatSnackbarHost(hostState = hostState, modifier = modifier)
}
