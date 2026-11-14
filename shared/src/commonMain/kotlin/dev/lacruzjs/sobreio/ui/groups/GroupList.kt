package dev.lacruzjs.sobreio.ui.groups

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_delete
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.action_leave
import dev.lacruzjs.sobreio.ui.components.GroupAvatar
import dev.lacruzjs.sobreio.ui.components.SwipeAction
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import org.jetbrains.compose.resources.stringResource

/** The groups, one row each. Swiping a row left reveals Edit (owner and admin) and Delete or Leave; one row is open at a time. */
@Composable
internal fun GroupList(groups: List<Group>, onEvent: (GroupsEvent) -> Unit) {
    var openGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openGroupId = null }

    val editLabel = stringResource(Res.string.action_edit)
    val deleteLabel = stringResource(Res.string.action_delete)
    val leaveLabel = stringResource(Res.string.action_leave)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(groups, key = { it.id }) { group ->
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GroupAvatar(name = group.name)
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = group.currencyCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
