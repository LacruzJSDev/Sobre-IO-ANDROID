package dev.lacruzjs.sobreio.ui.bankimport.picker

import androidx.compose.runtime.Composable

/** A file the user picked: its name (for the import record) and its bytes. */
class PickedFile(val name: String, val bytes: ByteArray)

/** Files above this size are not bank statements of one account; the picker refuses them. */
const val MAX_STATEMENT_BYTES = 5 * 1024 * 1024

/** What came back from the system picker. */
sealed interface PickResult {
    data class Picked(val file: PickedFile) : PickResult
    data object Cancelled : PickResult
    data object TooLarge : PickResult
    data object Unreadable : PickResult
}

/**
 * Opens the system file picker for a CSV statement (Android's document picker, iOS's document picker). It is a
 * platform API, so only the Route holds it and passes the result on as an event. Returns the function that opens it.
 */
@Composable
expect fun rememberStatementFilePicker(onResult: (PickResult) -> Unit): () -> Unit
