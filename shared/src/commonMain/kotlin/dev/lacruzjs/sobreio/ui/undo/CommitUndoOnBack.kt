package dev.lacruzjs.sobreio.ui.undo

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * While a delete waits for its snackbar ([pending]), system back and the back gesture go through [onLeave] like the
 * back arrow, so the ViewModel makes the delete final (registered in [PendingUndoCommits]) before the screen below
 * reloads. Without it the delete is only committed in `onCleared`, after the exit animation, and the screen below
 * shows figures from before it. With nothing pending, back is left to the navigation (predictive gesture included).
 */
@Composable
fun CommitUndoOnBack(pending: Boolean, onLeave: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = pending,
        onBackCompleted = onLeave,
    )
}
