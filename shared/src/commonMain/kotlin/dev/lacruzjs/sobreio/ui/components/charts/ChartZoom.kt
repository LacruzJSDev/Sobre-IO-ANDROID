package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_close
import dev.lacruzjs.sobreio.generated.resources.chart_enlarge
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/*
 * A chart grows from its card to the whole screen (decided 2026-11-19, plan phase 5.1). The host, installed once at the
 * root by `App`, holds ONE zoomed chart at a time and draws it above everything; the card and the full-screen page share
 * their bounds ([SharedTransitionScope.sharedElementWithCallerManagedVisibility]), so the card visibly grows into the page
 * and shrinks back on close (the X, system back or the back gesture). The details stay light: the card shows the
 * chart, the page shows it big with the figures behind it.
 */

private const val ZOOM_MILLIS = 350

/** Height of a chart on its zoomed page. */
val ZoomedChartHeight = 360.dp

/** What is zoomed: the card it grew from ([key]), the page title and the page's content. */
internal class ZoomRequest(val key: String, val title: String, val content: @Composable ColumnScope.() -> Unit)

@Stable
class ChartZoomState internal constructor() {
    internal var request by mutableStateOf<ZoomRequest?>(null)
    /** True while the page is (or is becoming) the visible one; false while it shrinks back. */
    internal var expanded by mutableStateOf(false)

    internal fun open(key: String, title: String, content: @Composable ColumnScope.() -> Unit) {
        request = ZoomRequest(key, title, content)
        expanded = true
    }

    internal fun close() {
        expanded = false
    }

    internal fun isOpen(key: String) = expanded && request?.key == key
}

private val LocalChartZoom = staticCompositionLocalOf<ChartZoomState?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalZoomScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Installed once around the whole app: everything inside may zoom a chart. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChartZoomHost(content: @Composable () -> Unit) {
    val state = remember { ChartZoomState() }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalChartZoom provides state, LocalZoomScope provides this) {
            Box(Modifier.fillMaxSize()) {
                content()
                state.request?.let { request -> ZoomedPage(state, request, this@SharedTransitionLayout) }
            }
        }
    }
}

/**
 * The card side of a zoom: [key] is unique on the screen; [content] is the compact chart (or its whole card). Without a
 * host (previews) it is a plain box.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ZoomableChart(key: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = LocalChartZoom.current
    val scope = LocalZoomScope.current
    if (state == null || scope == null) {
        Box(modifier) { content() }
        return
    }
    with(scope) {
        Box(
            modifier.sharedElementWithCallerManagedVisibility(
                sharedContentState = rememberSharedContentState(key),
                visible = !state.isOpen(key),
            ),
        ) { content() }
    }
}

/** Opens [key]'s page with [title] and [content]; a no-op without a host. */
@Composable
fun rememberChartZoom(key: String, title: String, content: @Composable ColumnScope.() -> Unit): () -> Unit {
    val state = LocalChartZoom.current
    val latestContent by rememberUpdatedState(content)
    val latestTitle by rememberUpdatedState(title)
    return remember(state, key) { { state?.open(key, latestTitle) { latestContent() } } }
}

/** The "Enlarge" icon at the right of a chart section's title (`FormSection(action = …)`). */
@Composable
fun ChartZoomButton(onClick: () -> Unit) {
    IconActionButton(icon = Icons.Filled.OpenInFull, label = stringResource(Res.string.chart_enlarge), onClick = onClick)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ZoomedPage(state: ChartZoomState, request: ZoomRequest, scope: SharedTransitionScope) {
    // Once it has shrunk back, the page leaves the composition
    LaunchedEffect(state.expanded, request) {
        if (!state.expanded) {
            delay(ZOOM_MILLIS + 100L)
            if (!state.expanded) state.request = null
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = state.expanded,
        onBackCompleted = { state.close() },
    )
    val contentAlpha by animateFloatAsState(if (state.expanded) 1f else 0f, tween(ZOOM_MILLIS))
    with(scope) {
        Box(
            Modifier
                .fillMaxSize()
                .sharedElementWithCallerManagedVisibility(
                    sharedContentState = rememberSharedContentState(request.key),
                    visible = state.expanded,
                )
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .contentMaxWidth(ContentWidth.TwoColumns),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { state.close() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                    }
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = request.content,
                )
            }
        }
    }
}
