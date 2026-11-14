package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SwipeValue { Closed, Open }

private val ActionWidth = 64.dp

/**
 * A list row that swipes left to reveal [actions] on its right (Edit, Delete...), the pattern of every list in the app.
 * It is controlled so that a list can keep a single row open: pass [isOpen] and react to [onOpenChange], which also
 * reports the user dragging the row open or closed. While open, tapping the row closes it instead of calling
 * [onClick]; tapping an action runs it and closes the row. [content] must not set its own background: the row paints
 * the app background so the actions stay hidden behind it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeActionsRow(
    actions: List<SwipeAction>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val revealPx = with(density) { (ActionWidth * actions.size).toPx() }
    // A row always starts closed, and starts again closed when the number of actions changes (new anchors): the caller
    // hears about it through the report below, so its idea of "open" can never outlive the drawn row
    val state = remember(revealPx) {
        AnchoredDraggableState(
            initialValue = SwipeValue.Closed,
            anchors = DraggableAnchors {
                SwipeValue.Closed at 0f
                SwipeValue.Open at -revealPx
            },
        )
    }
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)
    val currentIsOpen by rememberUpdatedState(isOpen)
    val scope = rememberCoroutineScope()

    // The caller closes this row (another one opened, the list scrolled) or opens it
    LaunchedEffect(state, isOpen) {
        val target = if (isOpen) SwipeValue.Open else SwipeValue.Closed
        if (state.targetValue != target) state.animateTo(target)
    }
    // What is drawn is reported to the caller whenever it settles and the caller does not know it yet
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { value ->
            val open = value == SwipeValue.Open
            if (open != currentIsOpen) currentOnOpenChange(open)
        }
    }
    // Open as far as the user can see: dragged open or still sliding back
    fun isDrawnOpen(): Boolean = state.targetValue == SwipeValue.Open || state.currentValue == SwipeValue.Open

    // While drawn open, a press anywhere outside this row closes it (app-wide rule, see SwipeRowsHost)
    val host = LocalSwipeRowsHost.current
    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val drawnOpen by remember(state) { derivedStateOf { isDrawnOpen() } }
    val registrationKey = remember { Any() }
    DisposableEffect(host, drawnOpen) {
        if (host != null && drawnOpen) {
            host.register(
                key = registrationKey,
                coordinates = { rowCoordinates },
                close = {
                    scope.launch { state.animateTo(SwipeValue.Closed) }
                    currentOnOpenChange(false)
                },
            )
        }
        onDispose { host?.unregister(registrationKey) }
    }

    val shape = MaterialTheme.shapes.medium
    Box(modifier = modifier.fillMaxWidth().clip(shape).onGloballyPositioned { rowCoordinates = it }) {
        Row(
            modifier = Modifier
                .matchParentSize()
                // Antialiased rounded corners would peek out at the row's edge while it is closed
                .drawWithContent { if (state.offset.takeIf { !it.isNaN() }?.let { it < 0f } == true) drawContent() },
            horizontalArrangement = Arrangement.End,
        ) {
            actions.forEach { action ->
                LabelTooltip(label = action.label) {
                    Box(
                        modifier = Modifier
                            .width(ActionWidth)
                            .fillMaxHeight()
                            .background(action.containerColor)
                            .clickable(onClickLabel = action.label) {
                                action.onClick()
                                currentOnOpenChange(false)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = action.contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(state.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0, 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(MaterialTheme.colorScheme.background)
                .clickable {
                    // Decided by the drawn state, not by the caller's copy of it, so a tap on an open row always closes it
                    if (isDrawnOpen()) {
                        scope.launch { state.animateTo(SwipeValue.Closed) }
                        currentOnOpenChange(false)
                    } else {
                        onClick()
                    }
                }
                .semantics {
                    // Screen readers cannot swipe: they get the same actions as a list
                    customActions = actions.map { action ->
                        CustomAccessibilityAction(action.label) {
                            action.onClick()
                            true
                        }
                    }
                },
        ) {
            content()
        }
    }
}
