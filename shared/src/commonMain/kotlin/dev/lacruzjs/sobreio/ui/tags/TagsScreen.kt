package dev.lacruzjs.sobreio.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.TagUsage
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.tag_new_title
import dev.lacruzjs.sobreio.generated.resources.tags_empty
import dev.lacruzjs.sobreio.generated.resources.tags_movements_count
import dev.lacruzjs.sobreio.generated.resources.tags_title
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.TagDot
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.mergeAction
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TagsRoute(
    groupId: String,
    onBack: () -> Unit,
    /** Opened from the "+" of an empty Tags section: the create sheet opens once the group has loaded. */
    startWithCreate: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Saved so rotating or coming back does not open the sheet again
    var createRequested by rememberSaveable { mutableStateOf(startWithCreate) }
    LaunchedEffect(createRequested, state.canEdit) {
        if (createRequested && state.canEdit) {
            viewModel.onEvent(TagsEvent.AddClicked)
            createRequested = false
        }
    }
    TagsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                TagsEvent.BackClicked -> onBack()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * The tags of a group: a colour dot, the name and how many movements carry it. Members of a live group swipe a live
 * row for Edit, Merge and Delete and an archived row for Reopen; create and edit use a short sheet.
 */
@Composable
fun TagsScreen(
    state: TagsUiState,
    onEvent: (TagsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.tags_title),
                subtitle = state.group?.name,
                onBack = { onEvent(TagsEvent.BackClicked) },
                actions = {
                    if (state.canEdit) {
                        AddIconButton(
                            contentDescription = stringResource(Res.string.tag_new_title),
                            onClick = { onEvent(TagsEvent.AddClicked) },
                        )
                    }
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(TagsEvent.Retry) }) { Text(stringResource(Res.string.action_retry)) }
                    }
                    state.tags.isEmpty() && state.archivedTags.isEmpty() ->
                        EmptyState(message = stringResource(Res.string.tags_empty))
                    else -> TagList(state, onEvent)
                }
            }
            UndoSnackbarHost(
                message = state.undoMessage,
                onUndo = { onEvent(TagsEvent.UndoClicked) },
                onDismiss = { onEvent(TagsEvent.UndoExpired) },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    when (state.sheet) {
        TagsSheet.None -> Unit
        TagsSheet.Edit -> TagSheet(state, onEvent)
        TagsSheet.Merge -> MergeSheet(state, onEvent)
    }
}

@Composable
private fun TagList(state: TagsUiState, onEvent: (TagsEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openTagId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openTagId = null }

    val rows: LazyListScope.(List<TagUsage>, Boolean) -> Unit = { list, archived ->
        items(list, key = { (if (archived) "archived-" else "") + it.id }) { tag ->
            val row: @Composable () -> Unit = { TagRow(tag) }
            if (state.canEdit) {
                SwipeActionsRow(
                    actions = if (archived) {
                        listOf(reopenAction { onEvent(TagsEvent.ReopenClicked(tag.id)) })
                    } else {
                        buildList {
                            add(editAction { onEvent(TagsEvent.EditClicked(tag.id)) })
                            if (state.canMerge) add(mergeAction { onEvent(TagsEvent.MergeClicked(tag.id)) })
                            add(deleteAction { onEvent(TagsEvent.DeleteClicked(tag.id)) })
                        }
                    },
                    isOpen = openTagId == tag.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openTagId = tag.id else if (openTagId == tag.id) openTagId = null
                    },
                    onClick = { if (!archived) onEvent(TagsEvent.EditClicked(tag.id)) },
                    content = row,
                )
            } else {
                row()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        rows(state.tags, false)
        if (state.archivedTags.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedTags.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            if (archivedExpanded) rows(state.archivedTags, true)
        }
    }
}

@Composable
private fun TagRow(tag: TagUsage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagDot(hex = tag.color, size = 14.dp)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val count = tag.movements.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            Text(
                text = pluralStringResource(Res.plurals.tags_movements_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TagsScreenPreview() {
    SobreioTheme {
        TagsScreen(
            state = TagsUiState(
                group = Group("g1", "Casa", "EUR", GroupRole.Member),
                tags = listOf(
                    TagUsage("t1", "Vacaciones", "#82B1D1", null, 12),
                    TagUsage("t2", "Reforma", "#E6AE8C", null, 1),
                ),
                archivedTags = listOf(TagUsage("t3", "Navidad", "#A99FD6", "2026-11-01T10:00:00Z", 4)),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
