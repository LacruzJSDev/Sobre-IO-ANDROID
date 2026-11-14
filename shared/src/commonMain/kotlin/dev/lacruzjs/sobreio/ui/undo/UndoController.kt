package dev.lacruzjs.sobreio.ui.undo

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ViewModel-side half of the undo pattern, reused by every list (groups, accounts, envelopes, movements): it holds the
 * one pending [UndoableAction], publishes the snackbar message through [onMessage] and runs commit or undo when the UI
 * reports the snackbar result. Only one action is pending at a time: starting another commits the previous one.
 */
class UndoController(
    private val scope: CoroutineScope,
    private val onMessage: (UndoMessage?) -> Unit,
    private val onError: (AppError) -> Unit,
) {
    private var pending: UndoableAction? = null
    private var nextKey = 0L

    /** The action's item is already hidden; shows the snackbar. */
    fun show(action: UndoableAction) {
        commit()
        pending = action
        onMessage(UndoMessage(++nextKey, action.message, action.argument))
    }

    /** The user tapped "Undo". */
    fun undo() {
        val action = take() ?: return
        scope.launch {
            action.undo()
                .onSuccess { action.restore() }
                .onFailure { onError(it.asAppError()) }
        }
    }

    /** The snackbar timed out or was dismissed, or something else needs the action to be final. */
    fun commit() {
        val action = take() ?: return
        scope.launch { commitAction(action) }
    }

    /** For `onCleared`: the ViewModel scope is already cancelled, so the commit runs on its own scope. */
    fun commitDetached() {
        val action = take() ?: return
        CoroutineScope(SupervisorJob()).launch { action.commit() }
    }

    private suspend fun commitAction(action: UndoableAction) {
        action.commit().onFailure {
            action.restore()
            onError(it.asAppError())
        }
    }

    private fun take(): UndoableAction? {
        val action = pending ?: return null
        pending = null
        onMessage(null)
        return action
    }
}
