package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Metadata key that marks a list pane entry (Group, Groups); see [SplitWhenDetailOpen]. */
internal const val ListPaneMarker: String = "dev.lacruzjs.sobreio.navigation.ListPane"

/** Metadata for a list pane entry, added to the list-detail strategy's own list metadata. */
internal val listPaneMarker: Map<String, Any> = mapOf(ListPaneMarker to true)

/**
 * Splits the window only while a detail is open. The list-detail strategy alone shows a list with an empty detail
 * pane beside it on wide windows; a list on top of the stack (nothing open next to it) takes the whole width instead,
 * so this returns no scene and the single-pane strategy shows it.
 */
internal class SplitWhenDetailOpen<T : Any>(private val inner: SceneStrategy<T>) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val top = entries.lastOrNull() ?: return null
        if (top.metadata.containsKey(ListPaneMarker)) return null
        return with(inner) { calculateScene(entries) }
    }
}
