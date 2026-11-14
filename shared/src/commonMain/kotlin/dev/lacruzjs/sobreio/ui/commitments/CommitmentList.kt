package dev.lacruzjs.sobreio.ui.commitments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.commitment_automatic
import dev.lacruzjs.sobreio.generated.resources.commitment_moves_to
import dev.lacruzjs.sobreio.generated.resources.commitment_next
import dev.lacruzjs.sobreio.generated.resources.commitment_starts_on
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatSignedMoney
import org.jetbrains.compose.resources.stringResource

/**
 * The commitments in their sections and, at the end, a collapsed "Archived" section. Owners and admins swipe an editable
 * live row left for Edit and Delete and an archived one for Reopen (one row open at a time) and tapping it edits; rows
 * they cannot edit (incoming transfers, received contributions, members' view) are plain.
 */
@Composable
internal fun CommitmentList(state: AccountCommitmentsUiState, onEvent: (CommitmentsEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openId = null }

    val languageTag = Locale.current.toLanguageTag()

    val rows: LazyListScope.(List<CommitmentRow>, Boolean) -> Unit = { list, archived ->
        items(list, key = { (if (archived) "archived-" else "") + it.id }) { row ->
            val content: @Composable () -> Unit = { CommitmentRowContent(row, state.currencyCode, languageTag) }
            if (row.editable) {
                SwipeActionsRow(
                    actions = if (archived) {
                        listOf(reopenAction { onEvent(CommitmentsEvent.ReopenClicked(row.id)) })
                    } else {
                        listOf(
                            editAction { onEvent(CommitmentsEvent.EditClicked(row.id)) },
                            deleteAction { onEvent(CommitmentsEvent.DeleteClicked(row.id)) },
                        )
                    },
                    isOpen = openId == row.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open -> if (open) openId = row.id else if (openId == row.id) openId = null },
                    // An archived row is read only until reopened
                    onClick = { if (!archived) onEvent(CommitmentsEvent.EditClicked(row.id)) },
                    content = content,
                )
            } else {
                Box { content() }
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
        state.sections.forEach { (section, list) ->
            item(key = "title-${section.name}") {
                Text(
                    text = stringResource(section.titleRes()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            rows(list, false)
        }
        if (state.archivedRows.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedRows.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            if (archivedExpanded) rows(state.archivedRows, true)
        }
    }
}

@Composable
private fun CommitmentRowContent(row: CommitmentRow, currencyCode: String, languageTag: String) {
    val frequency = frequencyText(row.version, languageTag)
    val next = row.nextDue?.let { stringResource(Res.string.commitment_next, formatDayMonth(it, languageTag)) }
    val automatic = if (row.version.autoRegister) stringResource(Res.string.commitment_automatic) else null
    val moves = if (row.movesToName != null && row.movesOn != null) {
        stringResource(Res.string.commitment_moves_to, row.movesToName, formatDate(row.movesOn, languageTag))
    } else null
    val starts = row.startsOn?.let { stringResource(Res.string.commitment_starts_on, formatDate(it, languageTag)) }
    val subtitle = listOfNotNull(frequency, starts, next, automatic, moves).reduce { acc, part ->
        stringResource(Res.string.row_subtitle, acc, part)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // No entity colour: a commitment is drawn in ink with the icon of its kind
        EntityAvatar(name = row.commitment.name, icon = row.version.kind.icon(), color = null)
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = row.commitment.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatSignedMoney(row.signedCents, currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
