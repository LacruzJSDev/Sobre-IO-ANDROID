package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.ui.components.displayName
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import dev.lacruzjs.sobreio.ui.components.RightColumnPadding
import dev.lacruzjs.sobreio.ui.components.LeftColumnPadding
import dev.lacruzjs.sobreio.ui.components.TwoColumnSplit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_clear_filters
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.filter_date_range
import dev.lacruzjs.sobreio.generated.resources.filter_tags_count
import dev.lacruzjs.sobreio.generated.resources.movements_empty
import dev.lacruzjs.sobreio.generated.resources.movements_empty_filtered
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ListSkeleton
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.FilterChipButton
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
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
        // Wide pane: the filters and the totals of what they pick on the left, the rows on the right
        TwoColumnSplit(
            modifier = Modifier.weight(1f),
            narrow = {
                Column(Modifier.fillMaxSize()) {
                    FilterChips(state, onEvent)
                    MovementRows(state, onEvent, withTotals = true, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp))
                }
            },
            left = {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LeftColumnPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    FilterChipsFlow(state, onEvent)
                    if (state.showsTotals) TotalsCard(state, onEvent) else TotalsHint()
                }
            },
            right = { MovementRows(state, onEvent, withTotals = false, contentPadding = RightColumnPadding) },
            leftWeight = 5f,
            rightWeight = 7f,
        )
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

/** The rows of the list with their loading, error and empty states; [withTotals] puts the totals card first. */
@Composable
private fun MovementRows(
    state: MovementsUiState,
    onEvent: (MovementsEvent) -> Unit,
    withTotals: Boolean,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {

        when {
            state.isLoading && state.items.isEmpty() -> ListSkeleton(withHero = false, rows = 8)
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
            else -> MovementList(state, onEvent, withTotals, contentPadding)
        }
    }
}

// Chips ------------------------------------------------------------------------------------------------------------

@Composable
private fun FilterChips(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(movementChips(state), key = { it.name }) { chip -> MovementFilterChip(chip, state, onEvent) }
    }
}

/** The same chips wrapped over several lines: the left column of a wide pane. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterChipsFlow(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        movementChips(state).forEach { chip -> MovementFilterChip(chip, state, onEvent) }
    }
}

private fun movementChips(state: MovementsUiState): List<MovementsChip> = buildList {
    // The level 3 list is one account's: there is nothing to choose
    if (state.fixedAccountId == null) add(MovementsChip.Account)
    if (state.fixedEnvelopeId == null) add(MovementsChip.Envelope)
    add(MovementsChip.Type)
    add(MovementsChip.Dates)
    add(MovementsChip.Tags)
    if (state.hasSeveralMembers) add(MovementsChip.Person)
}

@Composable
private fun MovementFilterChip(chip: MovementsChip, state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val filter = state.filter
    val label = stringResource(chip.labelRes())
    val account = state.accounts.firstOrNull { it.id == filter.accountId }
    val envelope = state.envelope(filter.envelopeId)
    val member = state.members.firstOrNull { it.userId == filter.createdBy }
    val activeText: String? = when (chip) {
        MovementsChip.Account -> account?.name
        MovementsChip.Envelope -> envelope?.displayName()
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
