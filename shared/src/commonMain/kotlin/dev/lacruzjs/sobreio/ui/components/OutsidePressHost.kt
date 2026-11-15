package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Pure bookkeeping of the transient UI that wants to close on an outside press: each entry has a key, its bounds in the
 * root (null when unknown or detached, which counts as outside) and a way to close itself. Kept apart from Compose so it
 * can be unit tested.
 */
class OutsidePressRegistry {
    private class Entry(val bounds: () -> Rect?, val close: () -> Unit)

    private val entries = mutableMapOf<Any, Entry>()

    fun register(key: Any, bounds: () -> Rect?, close: () -> Unit) {
        entries[key] = Entry(bounds, close)
    }

    fun unregister(key: Any) {
        entries.remove(key)
    }

    /** Closes every registered entry the press (in root coordinates) is not on. */
    fun onPress(pressInRoot: Offset) {
        entries.values.toList().forEach { entry ->
            val bounds = entry.bounds()
            if (bounds == null || !bounds.contains(pressInRoot)) entry.close()
        }
    }
}

/**
 * App-wide rule: transient UI (an open swipe row, a chart tooltip) closes as soon as the user presses anywhere outside
 * it (another row, the header, the tab bar, empty space). Each one registers here with its bounds and a way to close
 * itself while it is open; the host, installed once at the root of the UI, watches every press before the screen
 * handles it and closes what the press is not on. The press is not consumed, so what was touched still works (a tap on
 * a tab both closes the row and changes tab).
 */
class OutsidePressHost internal constructor() {
    private val registry = OutsidePressRegistry()
    internal var hostCoordinates: LayoutCoordinates? = null

    fun register(key: Any, coordinates: () -> LayoutCoordinates?, close: () -> Unit) {
        registry.register(key, { coordinates()?.takeIf { it.isAttached }?.boundsInRoot() }, close)
    }

    fun unregister(key: Any) {
        registry.unregister(key)
    }

    internal fun onPress(positionInHost: Offset) {
        val host = hostCoordinates?.takeIf { it.isAttached } ?: return
        registry.onPress(host.localToRoot(positionInHost))
    }
}

/** The host of the current UI, or null outside one (previews): nothing then closes on outside presses. */
val LocalOutsidePressHost = staticCompositionLocalOf<OutsidePressHost?> { null }

/** Installs the [OutsidePressHost] for [content]; the root of the app wraps everything in it once. */
@Composable
fun OutsidePressHostProvider(content: @Composable () -> Unit) {
    val host = remember { OutsidePressHost() }
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
        CompositionLocalProvider(LocalOutsidePressHost provides host) { content() }
    }
}

/**
 * Calls [onOutsidePress] when [active] and the user presses outside this element (see [OutsidePressHost]). The element's
 * own bounds are the "inside"; presses on it are left to it.
 */
@Composable
fun Modifier.closeOnOutsidePress(active: Boolean, onOutsidePress: () -> Unit): Modifier {
    val host = LocalOutsidePressHost.current
    val currentClose by rememberUpdatedState(onOutsidePress)
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val key = remember { Any() }
    DisposableEffect(host, active) {
        if (host != null && active) host.register(key, { coordinates }, { currentClose() })
        onDispose { host?.unregister(key) }
    }
    return onGloballyPositioned { coordinates = it }
}
