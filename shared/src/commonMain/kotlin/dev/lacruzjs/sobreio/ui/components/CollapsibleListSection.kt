package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.list_load_more_failed
import org.jetbrains.compose.resources.stringResource

private val BodyMaxHeight = 260.dp

/** How many rows before the end the next page is asked for. */
private const val LOAD_MORE_DISTANCE = 3

/**
 * A long list inside a detail screen, folded under a header: a soft tile with the [title], a muted [count] and a
 * chevron. Open, the rows ([itemContent]) sit under it in a box of bounded height with its own scroll, separated by
 * spacing. When [onLoadMore] is given, it is called as the user scrolls near the end; [isLoadingMore] shows a small
 * progress row and [loadMoreFailed] a message with a retry ([onLoadMore] again). [emptyText] shows instead of the rows
 * when there are none.
 */
@Composable
fun <T> CollapsibleListSection(
    title: String,
    count: Int,
    items: List<T>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    emptyText: String? = null,
    itemKey: ((T) -> Any)? = null,
    isLoadingMore: Boolean = false,
    loadMoreFailed: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    val expandedText = expandedStateText(expanded)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = expandedText }
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp).rotate(arrowRotation),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            if (items.isEmpty() && emptyText != null) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                SectionRows(items, itemKey, isLoadingMore, loadMoreFailed, onLoadMore, itemContent)
            }
        }
    }
}

@Composable
private fun <T> SectionRows(
    items: List<T>,
    itemKey: ((T) -> Any)?,
    isLoadingMore: Boolean,
    loadMoreFailed: Boolean,
    onLoadMore: (() -> Unit)?,
    itemContent: @Composable (T) -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 1 - LOAD_MORE_DISTANCE
        }
    }
    // The ViewModel ignores the call while a page is loading or when there is nothing left; after a failure it only
    // asks again from the retry button, because these keys do not change
    LaunchedEffect(nearEnd, items.size) {
        if (nearEnd && onLoadMore != null && !loadMoreFailed) onLoadMore()
    }
    // Bounded height: it scrolls on its own inside the scrolling screen
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().heightIn(max = BodyMaxHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (itemKey != null) {
            items(items, key = itemKey) { item -> itemContent(item) }
        } else {
            items(items) { item -> itemContent(item) }
        }
        if (isLoadingMore) {
            item(key = "loading-more") {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (loadMoreFailed) {
            item(key = "load-more-failed") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.list_load_more_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onLoadMore?.invoke() }) { Text(stringResource(Res.string.action_retry)) }
                }
            }
        }
    }
}
