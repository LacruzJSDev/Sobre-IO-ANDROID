package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.ui.components.ListSectionTitle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.lacruzjs.sobreio.ui.components.SingleColumnPadding
import dev.lacruzjs.sobreio.ui.components.RightColumnPadding
import dev.lacruzjs.sobreio.ui.components.LeftColumnPadding
import dev.lacruzjs.sobreio.ui.components.TwoColumnSplit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_target_per_month
import dev.lacruzjs.sobreio.generated.resources.envelope_target_starts_on
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_editor
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_member
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.holder_common
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.assignAction
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/**
 * The hero card at the top (it opens "Unassigned", which cannot be edited or deleted), the envelopes in sections by
 * type (monthly spending, bills, savings, free), each row with its progress bar and line, and a collapsed "Archived"
 * section at the end. Each live row swipes left: Assign for every member, Edit and Delete for owners and admins; archived
 * rows swipe Reopen (owners and admins). One row is open at a time. Tapping any row ("Unassigned" and the archived ones
 * included) opens the envelope detail.
 */
@Composable
internal fun EnvelopeList(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    // Wide pane: the money to distribute stays on the left while the envelopes scroll on the right
    TwoColumnSplit(
        narrow = { EnvelopeRows(state, onEvent, withHero = true, contentPadding = SingleColumnPadding) },
        left = {
            // Under the hero, the payments to come and the latest moves between envelopes
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LeftColumnPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.unassigned != null && state.summary != null) {
                    EnvelopesHero(state = state, onClick = { onEvent(EnvelopesEvent.OpenClicked(state.unassigned.id)) })
                }
                PaymentsSection(state)
                LatestMovesSection(state, onEvent)
            }
        },
        right = { EnvelopeRows(state, onEvent, withHero = false, contentPadding = RightColumnPadding) },
        leftWeight = 5f,
        rightWeight = 6f,
    )
}

@Composable
private fun EnvelopeRows(
    state: EnvelopesUiState,
    onEvent: (EnvelopesEvent) -> Unit,
    withHero: Boolean,
    contentPadding: PaddingValues,
) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openEnvelopeId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openEnvelopeId = null }

    val languageTag = Locale.current.toLanguageTag()

    val section: LazyListScope.(EnvelopePurpose, List<Envelope>) -> Unit = { purpose, list ->
        item(key = "section-$purpose") {
            ListSectionTitle(stringResource(purpose.labelRes()))
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
                onClick = { onEvent(EnvelopesEvent.OpenClicked(envelope.id)) },
            ) {
                EnvelopeRow(envelope, state, languageTag)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (withHero && state.unassigned != null && state.summary != null) {
            item(key = "hero") {
                EnvelopesHero(
                    state = state,
                    onClick = { onEvent(EnvelopesEvent.OpenClicked(state.unassigned.id)) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
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
        state.sections.forEach { (purpose, list) -> section(purpose, list) }
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
                            onClick = { onEvent(EnvelopesEvent.OpenClicked(envelope.id)) },
                            content = row,
                        )
                    } else {
                        Box(modifier = Modifier.clickable(role = Role.Button) { onEvent(EnvelopesEvent.OpenClicked(envelope.id)) }) { row() }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvelopeRow(envelope: Envelope, state: EnvelopesUiState, languageTag: String) {
    // A target that starts in a later period (D-53) has nothing to measure yet: the row says when it starts instead
    val startsLater = envelope.targetStartsAfter(state.today)
    val progress = state.progress[envelope.id]?.takeIf { !envelope.isArchived && !startsLater }
    val hasBar = progress?.hasBar == true
    // With a bar the line under it already says the goal, so the target only shows for envelopes without one
    val target = when {
        startsLater -> envelope.targetsFrom?.let { from ->
            stringResource(
                Res.string.envelope_target_starts_on,
                formatMoney(envelope.monthlyTargetCents, state.currencyCode, languageTag),
                formatDate(from, languageTag),
            )
        }
        envelope.monthlyTargetCents > 0 && !hasBar -> stringResource(
            Res.string.envelope_target_per_month,
            formatMoney(envelope.monthlyTargetCents, state.currencyCode, languageTag),
        )
        else -> null
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
            if (progress != null) EnvelopeProgressView(progress, state.currencyCode, languageTag)
        }
        MoneyText(
            amountCents = envelope.balanceCents,
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
