package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.foundation.layout.Column
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
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_target_per_month
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_editor
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_member
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_saving
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_spending
import dev.lacruzjs.sobreio.generated.resources.envelopes_total
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.holder_common
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.assignAction
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A compact total, then "Unassigned" as the first envelope (no swipe: it cannot be edited or deleted), the envelopes in
 * two sections, spending and saving, and a collapsed "Archived" section at the end. Each live row swipes left: Assign
 * for every member, Edit and Delete for owners and admins; archived rows swipe Reopen (owners and admins). One row is
 * open at a time. Tapping a row does nothing yet: the envelope detail is a later screen.
 */
@Composable
internal fun EnvelopeList(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openEnvelopeId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openEnvelopeId = null }

    val languageTag = Locale.current.toLanguageTag()

    val section: LazyListScope.(StringResource, List<Envelope>) -> Unit = { title, list ->
        if (list.isNotEmpty()) {
            item(key = "section-${list.first().kind}") {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { envelope ->
                val actions = buildList {
                    add(assignAction { onEvent(EnvelopesEvent.AssignClicked(envelope.id)) })
                    if (state.canEdit) {
                        add(editAction { onEvent(EnvelopesEvent.EditClicked(envelope.id)) })
                        add(deleteAction { onEvent(EnvelopesEvent.DeleteClicked(envelope.id)) })
                    }
                }
                SwipeActionsRow(
                    actions = actions,
                    isOpen = openEnvelopeId == envelope.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openEnvelopeId = envelope.id else if (openEnvelopeId == envelope.id) openEnvelopeId = null
                    },
                    // The detail screen does not exist yet
                    onClick = {},
                ) {
                    EnvelopeRow(envelope, state, languageTag)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "summary") { EnvelopesTotal(state, languageTag) }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        if (state.unassigned != null) {
            item(key = "unassigned") { UnassignedRow(state.unassignedCents, state.currencyCode, languageTag) }
        }
        if (state.envelopes.isEmpty() && state.archivedEnvelopes.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(
                        if (state.canEdit) Res.string.envelopes_empty_editor else Res.string.envelopes_empty_member,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 32.dp),
                )
            }
        }
        section(Res.string.envelopes_section_spending, state.spending)
        section(Res.string.envelopes_section_saving, state.saving)
        if (state.archivedEnvelopes.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedEnvelopes.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            if (archivedExpanded) {
                items(state.archivedEnvelopes, key = { "archived-${it.id}" }) { envelope ->
                    val row: @Composable () -> Unit = { EnvelopeRow(envelope, state, languageTag) }
                    if (state.canEdit) {
                        SwipeActionsRow(
                            actions = listOf(reopenAction { onEvent(EnvelopesEvent.ReopenClicked(envelope.id)) }),
                            isOpen = openEnvelopeId == envelope.id,
                            onOpenChange = { open ->
                                if (open) openEnvelopeId = envelope.id else if (openEnvelopeId == envelope.id) openEnvelopeId = null
                            },
                            onClick = {},
                            content = row,
                        )
                    } else {
                        row()
                    }
                }
            }
        }
    }
}

/** The money inside the envelopes as one compact line: label on the left, figure on the right. */
@Composable
private fun EnvelopesTotal(state: EnvelopesUiState, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.envelopes_total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMoney(state.totalCents, state.currencyCode, languageTag),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** "Unassigned" as one more envelope, the first, drawn exactly like the others (it only lacks the swipe actions). */
@Composable
private fun UnassignedRow(balanceCents: Long, currencyCode: String, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val name = stringResource(Res.string.envelopes_unassigned)
        EntityAvatar(name = name, icon = Icons.Filled.Inbox, color = null)
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp),
        )
        Text(
            text = formatMoney(balanceCents, currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun EnvelopeRow(envelope: Envelope, state: EnvelopesUiState, languageTag: String) {
    val target = if (envelope.monthlyTargetCents > 0) {
        stringResource(
            Res.string.envelope_target_per_month,
            formatMoney(envelope.monthlyTargetCents, state.currencyCode, languageTag),
        )
    } else {
        null
    }
    val holder = if (state.hasSeveralMembers) {
        state.holderName(envelope) ?: stringResource(Res.string.holder_common)
    } else {
        null
    }
    val subtitle = when {
        target != null && holder != null -> stringResource(Res.string.row_subtitle, target, holder)
        else -> target ?: holder
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(
            name = envelope.name,
            icon = iconFor(IconCategory.Envelope, envelope.icon),
            color = EntityColor.fromHex(envelope.color),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = envelope.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = formatMoney(envelope.balanceCents, state.currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
