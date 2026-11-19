package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.generated.resources.accounts_row_end_of_period
import dev.lacruzjs.sobreio.generated.resources.accounts_row_shortfall
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.accounts_in_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.accounts_outside_envelopes_title
import dev.lacruzjs.sobreio.generated.resources.accounts_hero_chart_description
import dev.lacruzjs.sobreio.generated.resources.accounts_hero_count
import dev.lacruzjs.sobreio.generated.resources.accounts_hero_last_months
import dev.lacruzjs.sobreio.generated.resources.accounts_hero_outside
import dev.lacruzjs.sobreio.generated.resources.accounts_hero_shared
import dev.lacruzjs.sobreio.generated.resources.accounts_total
import dev.lacruzjs.sobreio.generated.resources.groups_archived_title
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.HeroCard
import dev.lacruzjs.sobreio.ui.components.HeroFigure
import dev.lacruzjs.sobreio.ui.components.HeroLabel
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.charts.FreeProjectionChart
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.components.reopenAction
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The summary, the accounts in two sections (in the envelopes and outside them) and, at the end, a collapsed
 * "Archived" section. Owners and admins swipe a live row left for Edit and Delete and an archived row for Reopen (one
 * row open at a time); members get plain rows. Tapping a row opens the account's detail.
 */
@Composable
internal fun AccountList(state: AccountsUiState, onEvent: (AccountsEvent) -> Unit) {
    // Wide pane: the hero card stays on the left while the accounts scroll on the right
    TwoColumnSplit(
        narrow = { AccountRows(state, onEvent, withHero = true, contentPadding = SingleColumnPadding) },
        left = {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LeftColumnPadding)) {
                AccountsHero(state)
            }
        },
        right = { AccountRows(state, onEvent, withHero = false, contentPadding = RightColumnPadding) },
        leftWeight = 5f,
        rightWeight = 6f,
    )
}

@Composable
private fun AccountRows(
    state: AccountsUiState,
    onEvent: (AccountsEvent) -> Unit,
    withHero: Boolean,
    contentPadding: PaddingValues,
) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openAccountId by remember { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openAccountId = null }

    val languageTag = Locale.current.toLanguageTag()
    val outside = state.outsideEnvelopes

    val rows: LazyListScope.(List<Account>, Boolean) -> Unit = { list, archived ->
        items(list, key = { (if (archived) "archived-" else "") + it.id }) { account ->
            val open = { onEvent(AccountsEvent.OpenClicked(account.id)) }
            val row: @Composable () -> Unit = { AccountRow(account, state, languageTag) }
            if (state.canEdit) {
                SwipeActionsRow(
                    actions = if (archived) {
                        listOf(reopenAction { onEvent(AccountsEvent.ReopenClicked(account.id)) })
                    } else {
                        listOf(
                            editAction { onEvent(AccountsEvent.EditClicked(account.id)) },
                            deleteAction { onEvent(AccountsEvent.DeleteClicked(account.id)) },
                        )
                    },
                    isOpen = openAccountId == account.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openAccountId = account.id else if (openAccountId == account.id) openAccountId = null
                    },
                    onClick = open,
                    content = row,
                )
            } else {
                Box(modifier = Modifier.clickable(role = Role.Button, onClick = open)) { row() }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (withHero) item(key = "summary") { AccountsHero(state) }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        // The "In envelopes" title only helps when there is a second section to tell it from
        if (outside.isNotEmpty() && state.inEnvelopes.isNotEmpty()) {
            item(key = "title-in") { SectionTitle(stringResource(Res.string.accounts_in_envelopes_title)) }
        }
        rows(state.inEnvelopes, false)
        if (outside.isNotEmpty()) {
            item(key = "title-outside") { SectionTitle(stringResource(Res.string.accounts_outside_envelopes_title)) }
            rows(outside, false)
        }
        if (state.archivedAccounts.isNotEmpty()) {
            item(key = "archived-title") {
                Text(
                    text = stringResource(Res.string.groups_archived_title, state.archivedAccounts.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { archivedExpanded = !archivedExpanded }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            if (archivedExpanded) rows(state.archivedAccounts, true)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * The hero card of the tab: the total in accounts with, when the chart is shown, its period on the right; the chart of
 * the last 3 months; and, when some accounts are outside the envelopes, a footer with the two database figures and the
 * accounts behind each.
 */
@Composable
private fun AccountsHero(state: AccountsUiState) {
    val hero = LocalHeroColors.current
    val ink = hero.onContainer
    val languageTag = Locale.current.toLanguageTag()
    val showChart = state.balanceSeries.size > 1
    HeroCard(modifier = Modifier.padding(bottom = 8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroLabel(stringResource(Res.string.accounts_total), modifier = Modifier.weight(1f))
                if (showChart) HeroLabel(stringResource(Res.string.accounts_hero_last_months))
            }
            HeroFigure(amountCents = state.totalCents, currencyCode = state.currencyCode)
        }
        if (showChart) {
            val series = state.balanceSeries
            val points = remember(series) { series.map { LineChartPoint(it.day.toEpochDays(), it.balanceCents) } }
            // Only the past: today is its last point, so there is no dashed projection
            FreeProjectionChart(
                points = points,
                todayX = points.last().x,
                lineColor = ink,
                negativeColor = hero.negativeOnContainer,
                ringColor = hero.container,
                height = 56.dp,
                contentDescription = stringResource(
                    Res.string.accounts_hero_chart_description,
                    formatDayMonth(series.first().day, languageTag),
                    formatDayMonth(series.last().day, languageTag),
                    formatMoney(series.first().balanceCents, state.currencyCode, languageTag),
                    formatMoney(series.last().balanceCents, state.currencyCode, languageTag),
                ),
            )
        }
        if (state.outsideEnvelopes.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroFooterColumn(
                    label = stringResource(Res.string.accounts_hero_shared),
                    amountCents = state.inEnvelopesCents,
                    currencyCode = state.currencyCode,
                    ink = ink,
                    accounts = state.inEnvelopes,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f),
                )
                HeroFooterColumn(
                    label = stringResource(Res.string.accounts_hero_outside),
                    amountCents = state.outsideCents,
                    currencyCode = state.currencyCode,
                    ink = ink,
                    accounts = state.outsideEnvelopes,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A label, its database figure and a muted line naming the accounts: the name when it is one, otherwise their count. */
@Composable
private fun HeroFooterColumn(
    label: String,
    amountCents: Long,
    currencyCode: String,
    ink: Color,
    accounts: List<Account>,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        HeroLabel(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MoneyText(amountCents, currencyCode, MoneyRole.Plain, style = MaterialTheme.typography.titleMedium, maxLines = 1, ink = ink)
        val naming = when (accounts.size) {
            0 -> null
            1 -> accounts.first().name
            else -> pluralStringResource(Res.plurals.accounts_hero_count, accounts.size, accounts.size)
        }
        if (naming != null) {
            HeroLabel(
                text = naming,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                alpha = 0.6f,
            )
        }
    }
}

@Composable
private fun AccountRow(account: Account, state: AccountsUiState, languageTag: String) {
    val kind = stringResource(account.kind.labelRes())
    val subtitle = if (state.hasSeveralMembers) {
        val holder = state.holderName(account) ?: stringResource(Res.string.holder_joint)
        stringResource(Res.string.row_subtitle, kind, holder)
    } else {
        kind
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(
            name = account.name,
            icon = iconFor(IconCategory.Account, account.icon),
            color = EntityColor.fromHex(account.color),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Where it will be at the end of the period (account_forecast); a shortfall says so in the negative colour
            state.forecasts[account.id]?.takeIf { !account.isArchived }?.let { forecast ->
                val short = forecast.shortfallCents > 0
                Text(
                    text = if (short) {
                        stringResource(Res.string.accounts_row_shortfall, formatMoney(forecast.shortfallCents, state.currencyCode, languageTag))
                    } else {
                        stringResource(Res.string.accounts_row_end_of_period, formatMoney(forecast.projectedEndCents, state.currencyCode, languageTag))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (short) LocalMoneyColors.current.negative else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        MoneyText(
            amountCents = account.balanceCents,
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
