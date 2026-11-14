package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * App-wide rule: an open swipe row closes as soon as the user touches anywhere outside it (another row, the header, the
 * tab bar, empty space). Every open [SwipeActionsRow] registers here with its position and a way to close itself; the
 * host, installed once at the root of the UI, watches every press before the screen handles it and closes the rows the
 * press is not on. The press is not consumed, so what was touched still works (a tap on a tab both closes the row and
 * changes tab).
 */
class SwipeRowsHost internal constructor() {
    private class OpenRow(val coordinates: () -> LayoutCoordinates?, val close: () -> Unit)

    private val openRows = mutableMapOf<Any, OpenRow>()
    internal var hostCoordinates: LayoutCoordinates? = null

    internal fun register(key: Any, coordinates: () -> LayoutCoordinates?, close: () -> Unit) {
        openRows[key] = OpenRow(coordinates, close)
    }

    internal fun unregister(key: Any) {
        openRows.remove(key)
    }

    internal fun onPress(positionInHost: Offset) {
        val host = hostCoordinates?.takeIf { it.isAttached } ?: return
        val pressInRoot = host.localToRoot(positionInHost)
        openRows.values.toList().forEach { row ->
            val coordinates = row.coordinates()?.takeIf { it.isAttached }
            if (coordinates == null || !coordinates.boundsInRoot().contains(pressInRoot)) row.close()
        }
    }
}

/** The host of the current UI, or null outside one (previews): rows then simply do not close on outside taps. */
val LocalSwipeRowsHost = staticCompositionLocalOf<SwipeRowsHost?> { null }

/** Installs the [SwipeRowsHost] for [content]; the root of the app wraps everything in it once. */
@Composable
fun SwipeRowsHostProvider(content: @Composable () -> Unit) {
    val host = remember { SwipeRowsHost() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { host.hostCoordinates = it }
            .pointerInput(host) {
                awaitPointerEventScope {
                    while (true) {
                        // Initial pass: seen before any row, button or list handles (and possibly consumes) it
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            event.changes.firstOrNull()?.let { host.onPress(it.position) }
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalSwipeRowsHost provides host) { content() }
    }
}
