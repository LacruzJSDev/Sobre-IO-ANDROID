package dev.lacruzjs.sobreio.ui.undo

import org.jetbrains.compose.resources.StringResource

/**
 * A destructive action the user can undo. The item is already hidden from its list when this is created.
 *
 * - [commit] makes the action final; it runs when the snackbar times out, another destructive action starts, the screen
 *   is shown again or the ViewModel is cleared. If it fails the controller calls [restore] and reports the error.
 * - [undo] reverts it (a network call, or nothing when [commit] had not run yet); on success the controller calls
 *   [restore], on failure the item stays hidden (the database still has it deleted) and the error is reported.
 * - [restore] puts the item back in the list.
 */
class UndoableAction(
    val message: StringResource,
    val argument: String,
    val commit: suspend () -> Result<Unit>,
    val undo: suspend () -> Result<Unit>,
    val restore: () -> Unit,
)
