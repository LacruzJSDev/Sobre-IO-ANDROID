// Movements: the list grouped by day, its rows and their tags.

package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.ui.components.ListSectionTitle
import dev.lacruzjs.sobreio.ui.components.displayName
import dev.lacruzjs.sobreio.generated.resources.movement_no_tags
import dev.lacruzjs.sobreio.generated.resources.movement_tags_more
import dev.lacruzjs.sobreio.generated.resources.movement_row_aside
import dev.lacruzjs.sobreio.ui.components.TagDot
import dev.lacruzjs.sobreio.domain.model.Tag
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.list_load_more_failed
import dev.lacruzjs.sobreio.generated.resources.movement_from_account
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer
import dev.lacruzjs.sobreio.generated.resources.movement_to_account
import dev.lacruzjs.sobreio.generated.resources.movements_day_today
import dev.lacruzjs.sobreio.generated.resources.movements_day_yesterday
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

// List -------------------------------------------------------------------------------------------------------------

@Composable
internal fun MovementList(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    withTotals: Boolean,
    contentPadding: PaddingValues,
) {
    val languageTag = Locale.current.toLanguageTag()
    // Not saved: a row is always drawn closed when its screen is composed again
    var openId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openId = null }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 1 - LOAD_MORE_DISTANCE
        }
    }
    // The ViewModel ignores the call while a page is loading or when there is nothing left; after a failure it only
    // asks again from the retry button, because these keys do not change
    LaunchedEffect(nearEnd, state.items.size) {
        if (nearEnd && !state.loadMoreFailed) onEvent(MovementsEvent.LoadMore)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (withTotals && state.showsTotals) item(key = "totals") { TotalsCard(state, onEvent) }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        state.sections.forEach { section ->
            item(key = "day-${section.date}") { DayHeader(section, languageTag) }
            items(section.movements, key = { "${it.id}-${it.accountId}" }) { movement ->
                val row: @Composable () -> Unit = { MovementRow(movement, state, languageTag) }
                val actions = listOfNotNull(
                    if (movement.canEdit) editAction { onEvent(MovementsEvent.EditClicked(movement.id, movement.source)) } else null,
                    if (movement.canDelete) deleteAction { onEvent(MovementsEvent.DeleteClicked(movement.id)) } else null,
                )
                if (actions.isNotEmpty()) {
                    SwipeActionsRow(
                        actions = actions,
                        isOpen = openId == movement.id,
                        // A row reporting "closed" must not close another one that has just opened
                        onOpenChange = { open ->
                            if (open) openId = movement.id else if (openId == movement.id) openId = null
                        },
                        // Movements have no detail screen: they are edited by swiping
                        onClick = {},
                        content = row,
                    )
                } else {
                    row()
                }
            }
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (state.loadMoreFailed) {
            item(key = "load-more-failed") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.list_load_more_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onEvent(MovementsEvent.RetryLoadMore) }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }
        }
    }
}

private const val LOAD_MORE_DISTANCE = 8

@Composable
private fun DayHeader(section: DaySection, languageTag: String) {
    val date = formatDayMonth(section.date, languageTag)
    val text = when (section.relative) {
        DayRelative.Today -> stringResource(Res.string.movements_day_today, date)
        DayRelative.Yesterday -> stringResource(Res.string.movements_day_yesterday, date)
        DayRelative.Other -> formatWeekdayDayMonth(section.date, languageTag)
    }
    ListSectionTitle(text)
}

/**
 * One movement: the ACCOUNT's avatar (the movement belongs to an account, so rows differ and relate to it), then
 * line 1 with the notes (or the kind) cut to leave room for the envelope next to them (or, for a transfer, the other
 * account), and line 2 always reserved for the tags so rows keep the same height: up to 3, or 2 and "+N".
 * "Unassigned" is not named: it is the default and would repeat on most rows.
 */
@Composable
private fun MovementRow(movement: Movement, state: MovementsUiState, languageTag: String) {
    // On an account's list each row is that account's side; in the group ledger a transfer is one row, shown unsigned
    val accountSide = state.filter.accountId != null
    val ledgerTransfer = movement.isAccountTransfer && !accountSide

    val account = state.accounts.firstOrNull { it.id == movement.accountId }
    val counterName = state.accountName(movement.counterAccountId)
    val envelope = state.envelope(movement.envelopeId)

    val title = movement.notes ?: stringResource(
        if (ledgerTransfer) Res.string.movement_kind_transfer else movement.kind.labelRes(),
    )
    // What sits next to the title: the envelope, or where a transfer goes or comes from
    val aside: String? = when {
        movement.isAccountTransfer -> counterName?.let {
            stringResource(
                if (ledgerTransfer || movement.kind == MovementKind.TransferOut) Res.string.movement_to_account
                else Res.string.movement_from_account,
                it,
            )
        }
        // On an envelope's own list it would repeat on every row
        state.fixedEnvelopeId != null -> null
        // A transaction without a known envelope is shown as Unassigned
        envelope == null -> stringResource(Res.string.envelopes_unassigned)
        else -> envelope.displayName()
    }
    val tags = movement.tagIds.mapNotNull { id -> state.tags.firstOrNull { it.id == id } }
    val amount = if (ledgerTransfer) kotlin.math.abs(movement.amountCents) else movement.amountCents

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(
            name = account?.name ?: title,
            icon = if (account == null) Icons.Filled.SwapHoriz else iconFor(IconCategory.Account, account.icon),
            color = EntityColor.fromHex(account?.color),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Row {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Takes what is left, never pushing the envelope out
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                )
                if (aside != null) {
                    Text(
                        text = stringResource(Res.string.movement_row_aside, aside),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp).widthIn(max = 130.dp).alignByBaseline(),
                    )
                }
            }
            MovementTags(tags)
        }
        MoneyText(
            amountCents = amount,
            currencyCode = state.currencyCode,
            role = if (ledgerTransfer) MoneyRole.Transfer else MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** Up to this many tags fit on line 2; with more, one fewer is shown and a "+N" chip says how many are left. */
private const val MAX_ROW_TAGS = 3

/** Line 2 of a movement: its tags as small chips, or "No tags", always at the same height so rows never shift. */
@Composable
private fun MovementTags(tags: List<Tag>) {
    Row(
        modifier = Modifier.padding(top = 4.dp).height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shown = if (tags.size > MAX_ROW_TAGS) tags.take(MAX_ROW_TAGS - 1) else tags
        shown.forEach { tag ->
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TagDot(tag.color, size = 7.dp)
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 90.dp),
                )
            }
        }
        if (tags.isEmpty()) {
            Text(
                text = stringResource(Res.string.movement_no_tags),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tags.size > shown.size) {
            Text(
                text = stringResource(Res.string.movement_tags_more, tags.size - shown.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
