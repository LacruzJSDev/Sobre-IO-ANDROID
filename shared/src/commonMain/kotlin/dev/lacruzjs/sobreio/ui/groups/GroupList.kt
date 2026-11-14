package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_cancel_deletion
import dev.lacruzjs.sobreio.generated.resources.action_delete
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.action_leave
import dev.lacruzjs.sobreio.generated.resources.action_reopen
import dev.lacruzjs.sobreio.generated.resources.group_archived_label
import dev.lacruzjs.sobreio.generated.resources.group_deletion_date
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeAction
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/**
 * The groups, one row each. Swiping a row left reveals Edit (owner and admin) and Delete or Leave; one row is open at a
 * time. At the end, a collapsed "Archived" section lists archived groups (owners swipe Reopen) and the ones whose
 * deletion was requested (owners swipe Cancel deletion).
 */
@Composable
internal fun GroupList(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    var openGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openGroupId = null }

    val languageTag = Locale.current.toLanguageTag()
    val editLabel = stringResource(Res.string.action_edit)
    val deleteLabel = stringResource(Res.string.action_delete)
    val leaveLabel = stringResource(Res.string.action_leave)
    val reopenLabel = stringResource(Res.string.action_reopen)
    val cancelDeletionLabel = stringResource(Res.string.action_cancel_deletion)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(state.groups, key = { it.id }) { group ->
            val actions = buildList {
                if (group.canEdit) {
                    add(
                        SwipeAction(
                            label = editLabel,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { onEvent(GroupsEvent.EditClicked(group.id)) },
                        ),
                    )
                }
                add(
                    SwipeAction(
                        label = if (group.isOwner) deleteLabel else leaveLabel,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        onClick = { onEvent(GroupsEvent.DeleteClicked(group.id)) },
                    ),
                )
            }
            SwipeActionsRow(
                actions = actions,
                isOpen = openGroupId == group.id,
                // A row reporting "closed" must not close another one that has just opened
                onOpenChange = { open ->
                    if (open) openGroupId = group.id else if (openGroupId == group.id) openGroupId = null
                },
                onClick = { onEvent(GroupsEvent.GroupClicked(group.id)) },
            ) {
                GroupRow(group, subtitle = group.currencyCode)
            }
        }
        if (state.archivedGroups.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedGroups.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }
        if (archivedExpanded) {
            items(state.archivedGroups, key = { "archived-${it.id}" }) { group ->
                val pending = group.isPendingDeletion
                val subtitle = if (pending) {
                    stringResource(
                        Res.string.group_deletion_date,
                        group.deletionDate?.let { formatDate(it, languageTag) }.orEmpty(),
                    )
                } else {
                    stringResource(Res.string.group_archived_label)
                }
                val action = when {
                    !group.isOwner -> null
                    pending -> SwipeAction(
                        label = cancelDeletionLabel,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onEvent(GroupsEvent.CancelDeletionClicked(group.id)) },
                    )
                    else -> SwipeAction(
                        label = reopenLabel,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onEvent(GroupsEvent.ReopenClicked(group.id)) },
                    )
                }
                // A group pending deletion cannot be opened; an archived one opens read only
                val onClick = { if (!pending) onEvent(GroupsEvent.GroupClicked(group.id)) }
                if (action != null) {
                    SwipeActionsRow(
                        actions = listOf(action),
                        isOpen = openGroupId == group.id,
                        onOpenChange = { open ->
                            if (open) openGroupId = group.id else if (openGroupId == group.id) openGroupId = null
                        },
                        onClick = onClick,
                    ) {
                        GroupRow(group, subtitle)
                    }
                } else {
                    Column(modifier = Modifier.clickable(enabled = !pending, onClick = onClick)) { GroupRow(group, subtitle) }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(group: Group, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(
            name = group.name,
            icon = iconFor(IconCategory.Group, group.icon),
            color = EntityColor.fromHex(group.color),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
