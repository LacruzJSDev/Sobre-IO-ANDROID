package dev.lacruzjs.sobreio.ui.undo

import org.jetbrains.compose.resources.StringResource

/**
 * What the undo snackbar shows: [message] is a string resource with one `%1$s` placeholder filled with [argument] (the
 * item's name). [key] changes with every message, so a new one replaces the one on screen.
 */
data class UndoMessage(val key: Long, val message: StringResource, val argument: String)
