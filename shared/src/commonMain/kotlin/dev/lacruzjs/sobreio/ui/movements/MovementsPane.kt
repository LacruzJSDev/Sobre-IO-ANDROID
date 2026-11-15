package dev.lacruzjs.sobreio.ui.movements

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Inbox
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
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementType
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_clear_filters
import dev.lacruzjs.sobreio.generated.resources.action_new_movement
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.entity_all_accounts
import dev.lacruzjs.sobreio.generated.resources.entity_all_envelopes
import dev.lacruzjs.sobreio.generated.resources.entity_all_people
import dev.lacruzjs.sobreio.generated.resources.entity_all_types
import dev.lacruzjs.sobreio.generated.resources.entity_any_date
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.filter_account
import dev.lacruzjs.sobreio.generated.resources.filter_date_range
import dev.lacruzjs.sobreio.generated.resources.filter_envelope
import dev.lacruzjs.sobreio.generated.resources.filter_person
import dev.lacruzjs.sobreio.generated.resources.filter_tags
import dev.lacruzjs.sobreio.generated.resources.filter_tags_count
import dev.lacruzjs.sobreio.generated.resources.filter_tags_empty
import dev.lacruzjs.sobreio.generated.resources.filter_type
import dev.lacruzjs.sobreio.generated.resources.filter_dates
import dev.lacruzjs.sobreio.generated.resources.list_load_more_failed
import dev.lacruzjs.sobreio.generated.resources.movement_from_account
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer
import dev.lacruzjs.sobreio.generated.resources.movement_to_account
import dev.lacruzjs.sobreio.generated.resources.movement_transfer_route
import dev.lacruzjs.sobreio.generated.resources.movements_day_today
import dev.lacruzjs.sobreio.generated.resources.movements_day_yesterday
import dev.lacruzjs.sobreio.generated.resources.movements_empty
import dev.lacruzjs.sobreio.generated.resources.movements_empty_filtered
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.EntityMultiPickerSheet
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntityPickerSheet
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FilterChipButton
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Content shared by the Movements tab and the level 3 list of an account: the filter chips, the list grouped by day and
 * the sheets of the chips. The header (with the search and the "+") belongs to whoever holds this pane.
 */
@Composable
fun MovementsPane(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FilterChips(state, onEvent)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                state.isLoading && state.items.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && state.items.isEmpty() -> EmptyState(message = stringResource(state.error.messageRes())) {
                    TextButton(onClick = { onEvent(MovementsEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                state.items.isEmpty() -> if (state.hasActiveFilters) {
                    EmptyState(message = stringResource(Res.string.movements_empty_filtered)) {
                        TextButton(onClick = { onEvent(MovementsEvent.ClearAllFilters) }) {
                            Text(stringResource(Res.string.action_clear_filters))
                        }
                    }
                } else {
                    EmptyState(message = stringResource(Res.string.movements_empty))
                }
                else -> MovementList(state, onEvent)
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage?.let { message ->
                val languageTag = Locale.current.toLanguageTag()
                message.amountCents
                    ?.let { cents -> message.copy(argument = formatMoney(cents, state.currencyCode, languageTag)) }
                    ?: message
            },
            onUndo = { onEvent(MovementsEvent.UndoClicked) },
            onDismiss = { onEvent(MovementsEvent.UndoExpired) },
        )
    }
    MovementsSheets(state, onEvent)
}

// Chips ------------------------------------------------------------------------------------------------------------

@Composable
private fun FilterChips(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val filter = state.filter
    val chips = buildList {
        // The level 3 list is one account's: there is nothing to choose
        if (state.fixedAccountId == null) add(MovementsChip.Account)
        if (state.fixedEnvelopeId == null) add(MovementsChip.Envelope)
        add(MovementsChip.Type)
        add(MovementsChip.Dates)
        add(MovementsChip.Tags)
        if (state.hasSeveralMembers) add(MovementsChip.Person)
    }
    val unassignedName = stringResource(Res.string.envelopes_unassigned)
    LazyRow(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.name }) { chip ->
            val label = stringResource(chip.labelRes())
            val account = state.accounts.firstOrNull { it.id == filter.accountId }
            val envelope = state.envelope(filter.envelopeId)
            val member = state.members.firstOrNull { it.userId == filter.createdBy }
            val activeText: String? = when (chip) {
                MovementsChip.Account -> account?.name
                MovementsChip.Envelope -> envelope?.let { if (it.kind == EnvelopeKind.System) unassignedName else it.name }
                MovementsChip.Type -> filter.type?.let { stringResource(it.labelRes()) }
                MovementsChip.Dates -> if (filter.from != null && filter.to != null) {
                    stringResource(
                        Res.string.filter_date_range,
                        formatDayMonth(filter.from, languageTag),
                        formatDayMonth(filter.to, languageTag),
                    )
                } else {
                    null
                }
                MovementsChip.Tags -> when (filter.tagIds.size) {
                    0 -> null
                    1 -> state.tags.firstOrNull { it.id in filter.tagIds }?.name
                        ?: stringResource(Res.string.filter_tags_count, 1)
                    else -> stringResource(Res.string.filter_tags_count, filter.tagIds.size)
                }
                MovementsChip.Person -> member?.displayName
            }
            FilterChipButton(
                label = label,
                activeText = activeText,
                onClick = { onEvent(MovementsEvent.ChipClicked(chip)) },
                onClear = { onEvent(MovementsEvent.ChipCleared(chip)) },
                activeLeading = when (chip) {
                    MovementsChip.Account -> account?.let {
                        { EntityAvatar(it.name, iconFor(IconCategory.Account, it.icon), EntityColor.fromHex(it.color), size = 24.dp) }
                    }
                    MovementsChip.Envelope -> envelope?.let {
                        {
                            EntityAvatar(
                                name = it.name,
                                icon = if (it.kind == EnvelopeKind.System) Icons.Filled.Inbox else iconFor(IconCategory.Envelope, it.icon),
                                color = EntityColor.fromHex(it.color),
                                size = 24.dp,
                            )
                        }
                    }
                    MovementsChip.Person -> member?.let { { EntityAvatar(it.displayName, null, null, size = 24.dp) } }
                    else -> null
                },
            )
        }
    }
}

// List -------------------------------------------------------------------------------------------------------------

@Composable
private fun MovementList(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
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
        // The envelope is the screen's own, or the default one: naming it adds nothing
        state.fixedEnvelopeId != null -> null
        envelope == null || envelope.kind == EnvelopeKind.System -> null
        else -> envelope.name
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Takes what is left, never pushing the envelope out
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (aside != null) {
                    Text(
                        text = stringResource(Res.string.movement_row_aside, aside),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp).widthIn(max = 130.dp),
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

/** Line 2 of a movement: its tags as small chips, or the same empty height so the rows never shift. */
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
        if (tags.size > shown.size) {
            Text(
                text = stringResource(Res.string.movement_tags_more, tags.size - shown.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Sheets -----------------------------------------------------------------------------------------------------------

@Composable
private fun MovementsSheets(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val dismiss = { onEvent(MovementsEvent.SheetDismissed) }
    val allIcon = Icons.Filled.Apps
    when (state.sheet) {
        MovementsSheet.None -> Unit
        MovementsSheet.Account -> EntityPickerSheet(
            title = stringResource(Res.string.filter_account),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_accounts), allIcon),
            // Live accounts first, then the archived ones, whose movements are still in the ledger
            options = (state.accounts.filter { !it.isArchived } + state.accounts.filter { it.isArchived }).map(Account::toEntityOption),
            selectedId = state.filter.accountId,
            onSelect = { onEvent(MovementsEvent.AccountSelected(it)) },
            onDismiss = dismiss,
        )
        MovementsSheet.Envelope -> {
            val unassigned = stringResource(Res.string.envelopes_unassigned)
            EntityPickerSheet(
                title = stringResource(Res.string.filter_envelope),
                allOption = EntityOption(null, stringResource(Res.string.entity_all_envelopes), allIcon),
                options = (state.envelopes.filter { !it.isArchived } + state.envelopes.filter { it.isArchived }).map {
                    if (it.kind == EnvelopeKind.System) {
                        it.toEntityOption().copy(name = unassigned, icon = Icons.Filled.Inbox, color = null)
                    } else {
                        it.toEntityOption()
                    }
                },
                selectedId = state.filter.envelopeId,
                onSelect = { onEvent(MovementsEvent.EnvelopeSelected(it)) },
                onDismiss = dismiss,
            )
        }
        MovementsSheet.Type -> EntityPickerSheet(
            title = stringResource(Res.string.filter_type),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_types), allIcon),
            options = MovementType.entries.map { EntityOption(it.name, stringResource(it.labelRes()), it.icon()) },
            selectedId = state.filter.type?.name,
            onSelect = { id -> onEvent(MovementsEvent.TypeSelected(id?.let { MovementType.valueOf(it) })) },
            onDismiss = dismiss,
        )
        MovementsSheet.Dates -> EntityPickerSheet(
            title = stringResource(Res.string.filter_dates),
            allOption = EntityOption(null, stringResource(Res.string.entity_any_date), allIcon),
            options = DatePreset.entries
                .filter { it != DatePreset.PreviousPeriod || state.dateContext?.previousPeriod != null }
                .map { preset ->
                    val range = state.dateContext?.let { datePresetRange(preset, it) }
                    EntityOption(
                        id = preset.name,
                        name = stringResource(preset.labelRes()),
                        icon = allIcon,
                        trailing = range?.let { dateRangeText(it.first, it.second, languageTag) },
                    )
                },
            selectedId = state.datePreset?.name,
            onSelect = { id -> onEvent(MovementsEvent.DatePresetSelected(id?.let { DatePreset.valueOf(it) })) },
            onDismiss = dismiss,
        )
        MovementsSheet.Tags -> EntityMultiPickerSheet(
            title = stringResource(Res.string.filter_tags),
            options = state.tags.map { EntityOption(it.id, it.name, color = EntityColor.fromHex(it.color)) },
            selectedIds = state.filter.tagIds,
            onApply = { onEvent(MovementsEvent.TagsApplied(it)) },
            onDismiss = dismiss,
            emptyText = stringResource(Res.string.filter_tags_empty),
        )
        MovementsSheet.Person -> EntityPickerSheet(
            title = stringResource(Res.string.filter_person),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_people), allIcon),
            options = state.members.map { it.toEntityOption() },
            selectedId = state.filter.createdBy,
            onSelect = { onEvent(MovementsEvent.PersonSelected(it)) },
            onDismiss = dismiss,
        )
    }
    if (state.customRangeOpen) {
        CustomRangeDialog(
            from = state.filter.from,
            to = state.filter.to,
            onConfirm = { from, to -> onEvent(MovementsEvent.CustomRangeSelected(from, to)) },
            onDismiss = { onEvent(MovementsEvent.CustomRangeDismissed) },
        )
    }
}

@Composable
private fun dateRangeText(from: LocalDate, to: LocalDate, languageTag: String): String =
    stringResource(Res.string.filter_date_range, formatDayMonth(from, languageTag), formatDayMonth(to, languageTag))
