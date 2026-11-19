package dev.lacruzjs.sobreio.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateDefaultEnterTransition
import androidx.compose.material3.adaptive.layout.calculateDefaultExitTransition
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/** Metadata key holding an entry's pane in the list-detail scene ([listPane], [detailPane], [extraPane]). */
private const val PaneRoleKey: String = "dev.lacruzjs.sobreio.navigation.PaneRole"

/** Metadata of a list pane entry (Group, Groups): the root the details open beside. */
internal val listPane: Map<String, Any> = mapOf(PaneRoleKey to ListDetailPaneScaffoldRole.List)

/** Metadata of a detail pane entry (account, envelope, settings, review, profile). */
internal val detailPane: Map<String, Any> = mapOf(PaneRoleKey to ListDetailPaneScaffoldRole.Detail)

/** Metadata of an extra pane entry, opened from a detail (commitment, a filtered movements list, tags). */
internal val extraPane: Map<String, Any> = mapOf(PaneRoleKey to ListDetailPaneScaffoldRole.Extra)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val NavEntry<*>.paneRole: ThreePaneScaffoldRole?
    get() = metadata[PaneRoleKey] as? ThreePaneScaffoldRole

/**
 * List-detail on windows wide enough for two panes (from medium, see [directive]); on a phone it gives no scene and the
 * single-pane strategy shows every screen as before.
 *
 * It is ONE scene per root (the list entry: a group or Groups), with or without a detail open, so NavDisplay never
 * cross-fades while details open and close: the scaffold moves the panes itself. A list with nothing open takes the
 * whole width (the library's own strategy would keep an empty detail pane beside it), opening a detail narrows it while
 * the detail slides in, and closing one widens it back while the detail slides out WITH its content: the popped entry is
 * kept on screen until its pane has left (Navigation 3 cleans an entry up only once it leaves composition, so this is
 * safe). System back from a detail is a predictive gesture that scrubs the panes, then pops.
 *
 * [listWidth] is the list's width beside a detail; null shares the window in halves.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class AdaptiveListDetail<T : Any>(
    private val directive: PaneScaffoldDirective,
    private val listWidth: Dp?,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (directive.maxHorizontalPartitions < 2) return null
        if (entries.lastOrNull()?.paneRole == null) return null
        // The run of pane entries at the top of the stack; a full-screen entry (a form) below them ends it
        val paneEntries = entries.takeLastWhile { it.paneRole != null }
        val list = paneEntries.lastOrNull { it.paneRole == ListDetailPaneScaffoldRole.List } ?: return null
        val scaffoldEntries = paneEntries.drop(paneEntries.indexOf(list))
        return ListDetailScene(
            listEntry = list,
            entries = scaffoldEntries,
            previousEntries = entries.dropLast(1),
            directive = directive,
            listWidth = listWidth,
            onBack = onBack,
        )
    }
}

/** NavDisplay keeps a scene while its class and key stay: the key is the root, so a group and Groups animate apart. */
private data class ListDetailKey(val listContentKey: Any)

private data class ListDetailInfo(val key: Any, val entries: List<NavEntry<*>>) : NavigationEventInfo()

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class ListDetailScene<T : Any>(
    private val listEntry: NavEntry<T>,
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
    private val directive: PaneScaffoldDirective,
    private val listWidth: Dp?,
    private val onBack: () -> Unit,
) : Scene<T> {
    override val key: Any = ListDetailKey(listEntry.contentKey)

    // The list entry's metadata, so a root's own transition (Groups arrives like a back) applies when the root changes
    override val metadata: Map<String, Any> = listEntry.metadata

    // The strategy builds the scene only from entries that have a pane role, so none is dropped here
    private val history = entries.mapNotNull { entry ->
        entry.paneRole?.let { ThreePaneScaffoldDestinationItem(it, entry.contentKey) }
    }

    private val scaffoldValue: ThreePaneScaffoldValue = valueFor(history)

    /** What back leads to inside this scene, or null when back leaves it (the list alone: back leaves the app). */
    private val previousValue: ThreePaneScaffoldValue? =
        if (history.size > 1) valueFor(history.dropLast(1)).takeIf { it != scaffoldValue } else null

    /** A list alone is laid out with a single partition, so it fills the window instead of leaving an empty detail. */
    private fun valueFor(history: List<ThreePaneScaffoldDestinationItem<Any>>): ThreePaneScaffoldValue {
        val listAlone = history.all { it.pane == ListDetailPaneScaffoldRole.List }
        return calculateThreePaneScaffoldValue(
            maxHorizontalPartitions = if (listAlone) 1 else directive.maxHorizontalPartitions,
            adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
            destinationHistory = history,
        )
    }

    override val content: @Composable () -> Unit = { Content() }

    @Composable
    private fun Content() {
        val value = scaffoldValue
        // Remembered by position: NavDisplay keeps this slot while the key stays, so the state animates across stacks
        val scaffoldState = remember { MutableThreePaneScaffoldState(value) }

        val detail = entries.lastOrNull { it.paneRole == ListDetailPaneScaffoldRole.Detail }
        val extra = entries.lastOrNull { it.paneRole == ListDetailPaneScaffoldRole.Extra }
        // A popped detail or extra stays on screen while its pane slides out, then leaves composition
        var leavingDetail by remember { mutableStateOf<NavEntry<T>?>(null) }
        var leavingExtra by remember { mutableStateOf<NavEntry<T>?>(null) }
        SideEffect {
            if (detail != null) leavingDetail = detail
            if (extra != null) leavingExtra = extra
        }
        val shownDetail = detail ?: leavingDetail
        val shownExtra = extra ?: leavingExtra

        LaunchedEffect(value) {
            scaffoldState.animateTo(value)
            if (value[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden && detail == null) leavingDetail = null
            if (value[ListDetailPaneScaffoldRole.Extra] == PaneAdaptedValue.Hidden && extra == null) leavingExtra = null
        }

        // Predictive back: the gesture scrubs the panes towards what back leads to, and pops once it completes
        val previous = previousValue
        val info = remember(key, entries) { ListDetailInfo(key, entries) }
        val gestureState = rememberNavigationEventState(currentInfo = info)
        NavigationBackHandler(state = gestureState, isBackEnabled = previous != null, onBackCompleted = onBack)
        val transition = gestureState.transitionState
        LaunchedEffect(transition) {
            if (transition is NavigationEventTransitionState.InProgress && previous != null) {
                scaffoldState.seekTo(fraction = backFraction(transition.latestEvent.progress, value), targetState = previous)
            } else {
                scaffoldState.animateTo(value)
            }
        }

        ListDetailPaneScaffold(
            directive = directive,
            scaffoldState = scaffoldState,
            listPane = { Pane(listEntry, listWidthModifier()) },
            detailPane = { Pane(shownDetail) },
            extraPane = { Pane(shownExtra) },
        )
    }

    private fun ThreePaneScaffoldPaneScope.listWidthModifier(): Modifier =
        if (listWidth != null) Modifier.preferredWidth(listWidth) else Modifier.preferredWidth(0.5f)

    @Composable
    private fun ThreePaneScaffoldPaneScope.Pane(entry: NavEntry<T>?, modifier: Modifier = Modifier) {
        AnimatedPane(
            modifier = modifier,
            enterTransition = motionDataProvider.calculateDefaultEnterTransition(paneRole),
            exitTransition = motionDataProvider.calculateDefaultExitTransition(paneRole),
            boundsAnimationSpec = PaneMotionDefaults.AnimationSpec,
        ) {
            entry?.Content()
        }
    }
}

/** The library's mapping of the back gesture's progress to how far the panes move (Material's predictive back). */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun backFraction(progress: Float, value: ThreePaneScaffoldValue): Float {
    val expanded = listOf(
        ListDetailPaneScaffoldRole.List,
        ListDetailPaneScaffoldRole.Detail,
        ListDetailPaneScaffoldRole.Extra,
    ).count { value[it] == PaneAdaptedValue.Expanded }
    val ratio = when (expanded) {
        1 -> 0.1f
        2 -> 0.15f
        else -> 0.2f
    }
    return PredictiveBackEasing.transform(progress) * ratio
}

private val PredictiveBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
