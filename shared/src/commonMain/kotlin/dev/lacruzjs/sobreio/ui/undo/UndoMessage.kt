package dev.lacruzjs.sobreio.ui.undo

import org.jetbrains.compose.resources.StringResource

/**
 * What the undo snackbar shows: [message] is a string resource with one `%1$s` placeholder filled with [argument] (the
 * item's name). [key] changes with every message, so a new one replaces the one on screen.
 */
data class UndoMessage(
    val key: Long,
    val message: StringResource,
    val argument: String,
    /** False for a plain notice ("Role changed"): the snackbar then has no "Undo". */
    val undoable: Boolean = true,
    /**
     * An amount in cents that the message is about (an adjustment): the screen formats it, because the ViewModel knows
     * neither the language nor how money is written, and fills the placeholder with it instead of [argument].
     */
    val amountCents: Long? = null,
)
