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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

private enum class SwipeValue { Closed, Open }

private val ActionWidth = 88.dp

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
    val state = remember(revealPx) {
        AnchoredDraggableState(
            initialValue = if (isOpen) SwipeValue.Open else SwipeValue.Closed,
            anchors = DraggableAnchors {
                SwipeValue.Closed at 0f
                SwipeValue.Open at -revealPx
            },
        )
    }
    val currentOnOpenChange by rememberUpdatedState(onOpenChange)

    // The caller closes this row (another one opened, the list scrolled, a tap elsewhere) or opens it
    LaunchedEffect(state, isOpen) {
        val target = if (isOpen) SwipeValue.Open else SwipeValue.Closed
        if (state.targetValue != target) state.animateTo(target)
    }
    // The user's drag, reported once it settles
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { currentOnOpenChange(it == SwipeValue.Open) }
    }

    val shape = MaterialTheme.shapes.medium
    Box(modifier = modifier.fillMaxWidth().clip(shape)) {
        Row(
            modifier = Modifier
                .matchParentSize()
                // Antialiased rounded corners would peek out at the row's edge while it is closed
                .drawWithContent { if (state.offset.takeIf { !it.isNaN() }?.let { it < 0f } == true) drawContent() },
            horizontalArrangement = Arrangement.End,
        ) {
            actions.forEach { action ->
                Box(
                    modifier = Modifier
                        .width(ActionWidth)
                        .fillMaxHeight()
                        .background(action.containerColor)
                        .clickable {
                            action.onClick()
                            currentOnOpenChange(false)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = action.contentColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(state.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0, 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
                .background(MaterialTheme.colorScheme.background)
                .clickable { if (isOpen) currentOnOpenChange(false) else onClick() }
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
