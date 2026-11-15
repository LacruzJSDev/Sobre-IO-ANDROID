package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * Decides how a text field that owns its text ([TextFieldState]) stays in step with a String held outside (a
 * ViewModel `StateFlow`). The outside value comes back late, so a stale one must never overwrite what the user has
 * typed since: texts the field sent are remembered as pending, and an outside value that is one of them is only an
 * acknowledgement. Anything else is an external change and is applied. Pure, so it is unit tested.
 */
class TextSync(initial: String) {
    private val pending = ArrayDeque<String>()
    private val acknowledged = ArrayDeque<String>()
    private var known = initial

    /** The field's text changed. True when it is new and must be sent outside. */
    fun onFieldText(text: String): Boolean {
        if (text == known) return false
        known = text
        pending.addLast(text)
        return true
    }

    /** The outside value changed. Returns the text to put in the field, or null when nothing must change. */
    fun onOutsideValue(value: String): String? {
        val index = pending.indexOf(value)
        if (index >= 0) {
            repeat(index + 1) { acknowledged.addLast(pending.removeFirst()) }
            if (pending.isEmpty()) acknowledged.clear()
            return null
        }
        if (pending.isEmpty()) {
            if (value == known) return null
        } else if (value in acknowledged) {
            // An older value arriving after a newer one was acknowledged: stale
            return null
        }
        pending.clear()
        acknowledged.clear()
        known = value
        return value
    }
}

/**
 * A [TextFieldState] that mirrors [value] and reports typing through [onValueChange]. Typing never waits for the
 * outside value, so the cursor cannot jump when a stale value is recomposed.
 */
@Composable
internal fun rememberSyncedTextFieldState(value: String, onValueChange: (String) -> Unit): TextFieldState {
    val state = rememberTextFieldState(value)
    val sync = remember(state) { TextSync(state.text.toString()) }
    val latestOnChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state, sync) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (sync.onFieldText(text)) latestOnChange(text)
        }
    }
    LaunchedEffect(value, state, sync) {
        sync.onOutsideValue(value)?.let { state.setTextAndPlaceCursorAtEnd(it) }
    }
    return state
}

/** Keeps digits only, at most [maxLength] of them (the 6-digit codes). */
fun digitsOnly(maxLength: Int): InputTransformation = InputTransformation {
    val clean = asCharSequence().filter(Char::isDigit).take(maxLength).toString()
    if (clean != asCharSequence().toString()) replace(0, length, clean)
}

/** Amounts are typed positive: a minus sign never gets in. */
internal val noMinusSign: InputTransformation = InputTransformation {
    if (asCharSequence().contains('-')) replace(0, length, asCharSequence().toString().replace("-", ""))
}

