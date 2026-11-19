package dev.lacruzjs.sobreio.ui.groupsummary


import dev.lacruzjs.sobreio.generated.resources.info_free_today_3
import dev.lacruzjs.sobreio.generated.resources.info_free_today_2
import dev.lacruzjs.sobreio.generated.resources.info_free_today_1
import dev.lacruzjs.sobreio.ui.components.HeroLabelWithInfo
import dev.lacruzjs.sobreio.generated.resources.chart_enlarge
import dev.lacruzjs.sobreio.generated.resources.chart_table_free
import dev.lacruzjs.sobreio.ui.components.charts.DayLineChart
import androidx.compose.ui.unit.Dp
import dev.lacruzjs.sobreio.ui.components.charts.FlowsChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ChartTableRow
import dev.lacruzjs.sobreio.ui.components.charts.ChartTable
import dev.lacruzjs.sobreio.ui.components.charts.ZoomedChartHeight
import dev.lacruzjs.sobreio.ui.components.charts.ZoomableChart
import dev.lacruzjs.sobreio.ui.components.charts.ChartZoomButton
import dev.lacruzjs.sobreio.ui.components.charts.rememberChartZoom
import dev.lacruzjs.sobreio.generated.resources.balance_on_day_choose
import dev.lacruzjs.sobreio.generated.resources.outlook_until_label
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_free_on
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_note
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_title
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import androidx.compose.foundation.background
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_uncovered
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_move
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_leaves
import dev.lacruzjs.sobreio.generated.resources.transfer_hint_enters
import dev.lacruzjs.sobreio.generated.resources.transfer_action
import dev.lacruzjs.sobreio.domain.model.TransferHint
import dev.lacruzjs.sobreio.domain.model.BoundaryCrossing
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.chart_flows_description
import dev.lacruzjs.sobreio.generated.resources.chart_flows_period
import dev.lacruzjs.sobreio.generated.resources.chart_legend_in
import dev.lacruzjs.sobreio.generated.resources.chart_legend_out
import dev.lacruzjs.sobreio.generated.resources.detail_section_upcoming
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.group_hero_chart_description
import dev.lacruzjs.sobreio.generated.resources.group_hero_drops
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_commitments
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_contributions
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_income
import dev.lacruzjs.sobreio.generated.resources.group_hero_days_left
import dev.lacruzjs.sobreio.generated.resources.group_hero_last_day
import dev.lacruzjs.sobreio.generated.resources.group_hero_projected_on
import dev.lacruzjs.sobreio.generated.resources.group_review_count
import dev.lacruzjs.sobreio.generated.resources.group_review_empty
import dev.lacruzjs.sobreio.generated.resources.group_review_open
import dev.lacruzjs.sobreio.generated.resources.group_review_oldest
import dev.lacruzjs.sobreio.generated.resources.group_summary_flows_title
import dev.lacruzjs.sobreio.generated.resources.group_summary_free_today
import dev.lacruzjs.sobreio.generated.resources.group_summary_spending_all
import dev.lacruzjs.sobreio.generated.resources.group_summary_spending_title
import dev.lacruzjs.sobreio.generated.resources.group_summary_upcoming_empty
import dev.lacruzjs.sobreio.generated.resources.group_summary_warnings_title
import dev.lacruzjs.sobreio.generated.resources.warning_account_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_commitments_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_envelope_overspent
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ListSkeleton
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.TwoColumnLayout
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.HeroCard
import dev.lacruzjs.sobreio.ui.components.HeroFigure
import dev.lacruzjs.sobreio.ui.components.HeroLabel
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.FreeProjectionChart
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.format.formatWeekdayDayMonth
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Group tab: the hero card with the free money and its projection, the review card, the warnings, where the
 * period's money went, the money in and out by period and the next due dates. It lives inside the group shell.
 */
@Composable
fun GroupSummaryPane(
    state: GroupSummaryUiState,
    onEvent: (GroupSummaryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        val outlook = state.outlook
        when {
            state.isLoading -> ListSkeleton()
            state.error != null && outlook == null -> EmptyState(message = stringResource(state.error.messageRes())) {
                TextButton(onClick = { onEvent(GroupSummaryEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
            }
            outlook != null -> SummaryContent(state, outlook, onEvent)
        }
    }
}

@Composable
private fun SummaryContent(state: GroupSummaryUiState, outlook: GroupOutlook, onEvent: (GroupSummaryEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    // What needs attention now on the left (the free money, what to review, the warnings); how the period went and
    // what comes next on the right. Each column scrolls by itself on a wide pane.
    TwoColumnLayout(
        top = { ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp)) },
        left = {
            GroupHero(state, outlook, languageTag)
            ReviewCard(state, languageTag, onEvent)
            if (state.warnings.isNotEmpty()) WarningsCard(state, languageTag, onEvent)
            LookAheadSection(state, languageTag, onEvent)
        },
        right = {
            if (state.spending.isNotEmpty()) SpendingSection(state, onEvent)
            if (state.flows.isNotEmpty()) FlowsSection(state, languageTag)
            UpcomingSection(state, languageTag)
        },
    )
}

/**
 * "Look until": the same figures as the hero card but up to a chosen day (`group_outlook(p_until)`), for example the
 * next pay day or a date past the end of the period. Every figure is the database's.
 */
@Composable
private fun LookAheadSection(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    FormSection(title = stringResource(Res.string.group_look_ahead_title)) {
        DateFormTile(
            label = stringResource(Res.string.outlook_until_label),
            date = state.lookAheadDay,
            onDateSelected = { onEvent(GroupSummaryEvent.LookAheadSelected(it)) },
            emptyText = stringResource(Res.string.balance_on_day_choose),
            onClear = if (state.lookAheadDay != null) ({ onEvent(GroupSummaryEvent.LookAheadCleared) }) else null,
            note = if (state.lookAheadDay == null) stringResource(Res.string.group_look_ahead_note) else null,
        )
        val figures = state.lookAhead
        val day = state.lookAheadDay
        if (figures != null && day != null) {
            StatCard(
                currencyCode = state.currencyCode,
                rows = buildList {
                    add(StatRow(stringResource(Res.string.group_summary_free_today), figures.freeTodayCents, MoneyRole.Balance))
                    if (figures.committedShortfallCents > 0) {
                        add(StatRow(stringResource(Res.string.group_hero_note_commitments), -figures.committedShortfallCents, MoneyRole.Flow))
                    }
                    if (figures.pendingOutCents > 0) {
                        add(StatRow(stringResource(Res.string.group_hero_note_contributions), -figures.pendingOutCents, MoneyRole.Flow))
                    }
                    if (figures.pendingIncomeCents > 0) {
                        add(StatRow(stringResource(Res.string.group_hero_note_income), figures.pendingIncomeCents, MoneyRole.Flow))
                    }
                    add(
                        StatRow(
                            stringResource(Res.string.group_look_ahead_free_on, formatDate(day, languageTag)),
                            figures.freeProjectedCents,
                            MoneyRole.Balance,
                            emphasised = true,
                        ),
                    )
                },
            )
        }
    }
}

/**
 * The hero card: the really free money today, its projection to the end of the period, the projected figure with the
 * database figure that explains the difference, the lowest day when it goes below zero and how far into the period we
 * are. The big figure is always white; when it is negative a "Below zero" chip says so (`HeroFigure`).
 */
@Composable
private fun GroupHero(state: GroupSummaryUiState, outlook: GroupOutlook, languageTag: String) {
    val hero = LocalHeroColors.current
    val paper = hero.onContainer
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeroLabelWithInfo(
                text = stringResource(Res.string.group_summary_free_today),
                infoTitle = stringResource(Res.string.group_summary_free_today),
                paragraphs = listOf(Res.string.info_free_today_1, Res.string.info_free_today_2, Res.string.info_free_today_3),
                modifier = Modifier.weight(1f),
            )
            state.daysLeft?.let { left ->
                HeroLabel(
                    text = if (left == 0) {
                        stringResource(Res.string.group_hero_last_day)
                    } else {
                        pluralStringResource(Res.plurals.group_hero_days_left, left, left)
                    },
                )
            }
        }
        HeroFigure(amountCents = outlook.freeTodayCents, currencyCode = state.currencyCode)
        if (state.freeProjection.isNotEmpty() && state.today != null) {
            val points = remember(state.freeProjection) { state.freeProjection.map { LineChartPoint(it.day.toEpochDays(), it.freeCents) } }
            val first = state.freeProjection.first()
            val last = state.freeProjection.last()
            val description = stringResource(
                    Res.string.group_hero_chart_description,
                    formatDayMonth(first.day, languageTag),
                    formatDayMonth(last.day, languageTag),
                    formatMoney(first.freeCents, state.currencyCode, languageTag),
                    formatMoney(last.freeCents, state.currencyCode, languageTag),
                )
            val today = state.today
            // A tap opens the chart full screen: axes, crosshair and every day's figure
            val zoom = rememberChartZoom(ZOOM_GROUP_FREE, stringResource(Res.string.group_summary_free_today)) {
                SoftTile {
                    DayLineChart(points, today.toEpochDays(), stringResource(Res.string.chart_table_free), state.currencyCode, languageTag, description)
                }
                ChartTable(
                    columns = listOf(stringResource(Res.string.chart_table_free)),
                    rows = state.freeProjection.map { ChartTableRow(formatDate(it.day, languageTag), listOf(it.freeCents)) },
                    currencyCode = state.currencyCode,
                    role = MoneyRole.Balance,
                )
            }
            ZoomableChart(
                ZOOM_GROUP_FREE,
                Modifier.clickable(onClickLabel = stringResource(Res.string.chart_enlarge), role = Role.Button, onClick = zoom),
            ) {
                FreeProjectionChart(
                    points = points,
                    todayX = today.toEpochDays(),
                    lineColor = paper,
                    negativeColor = hero.negativeOnContainer,
                    ringColor = hero.container,
                    height = 56.dp,
                    contentDescription = description,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                HeroLabel(text = stringResource(Res.string.group_hero_projected_on, formatDayMonth(outlook.periodEnd, languageTag)))
                MoneyText(
                    amountCents = outlook.freeProjectedCents,
                    currencyCode = state.currencyCode,
                    role = MoneyRole.Plain,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    ink = paper,
                )
            }
            state.projectionNote?.let { note ->
                Column(horizontalAlignment = Alignment.End) {
                    HeroLabel(
                        text = stringResource(
                            when (note.kind) {
                                ProjectionNoteKind.Commitments -> Res.string.group_hero_note_commitments
                                ProjectionNoteKind.Contributions -> Res.string.group_hero_note_contributions
                                ProjectionNoteKind.Income -> Res.string.group_hero_note_income
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MoneyText(
                        amountCents = note.cents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Plain,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        ink = paper,
                    )
                }
            }
        }
        state.lowestPoint?.let { lowest ->
            Text(
                text = stringResource(
                    Res.string.group_hero_drops,
                    formatMoney(lowest.freeCents, state.currencyCode, languageTag),
                    formatDayMonth(lowest.day, languageTag),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = hero.negativeOnContainer,
            )
        }
    }
}

@Composable
private fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Always visible: a calm message when everything is reviewed, the count and one button to the review screen otherwise. */
@Composable
private fun ReviewCard(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    SoftCard {
        if (state.pendingCount == 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = stringResource(Res.string.group_review_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.group_review_count, state.pendingCount),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.oldestPendingDate?.let { date ->
                    Text(
                        text = stringResource(Res.string.group_review_oldest, formatDate(date, languageTag)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PrimaryButton(
                text = stringResource(Res.string.group_review_open),
                onClick = { onEvent(GroupSummaryEvent.ReviewClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WarningsCard(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    SoftCard {
        CardTitle(stringResource(Res.string.group_summary_warnings_title))
        state.warnings.forEach { warning ->
            val text = when (warning) {
                is GroupWarning.EnvelopeOverspent -> stringResource(
                    Res.string.warning_envelope_overspent,
                    warning.name,
                    formatMoney(warning.overspentCents, state.currencyCode, languageTag),
                )
                is GroupWarning.AccountShortfallOn -> stringResource(
                    Res.string.warning_account_shortfall,
                    warning.name,
                    formatMoney(warning.lowestCents, state.currencyCode, languageTag),
                    formatDate(warning.date, languageTag),
                )
                is GroupWarning.CommitmentsShortfall -> stringResource(
                    Res.string.warning_commitments_shortfall,
                    formatMoney(warning.cents, state.currencyCode, languageTag),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEvent(GroupSummaryEvent.WarningClicked(warning)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = LocalMoneyColors.current.negative,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (warning is GroupWarning.AccountShortfallOn) {
                    warning.hints.forEach { hint -> TransferHintRow(hint, state, languageTag, onEvent) }
                }
            }
        }
    }
}

/**
 * One suggestion of `transfer_plan` under its account's warning: "Move N from X before D" (muted note when it crosses the
 * envelope boundary) and a compact "Transfer" button that opens the movement form already filled; with no source account
 * it only says that nothing can cover the amount. The button is for members who may record, in a live group.
 */
@Composable
private fun TransferHintRow(
    hint: TransferHint,
    state: GroupSummaryUiState,
    languageTag: String,
    onEvent: (GroupSummaryEvent) -> Unit,
) {
    val suggestion = hint.suggestion
    val amount = formatMoney(suggestion.amountCents, state.currencyCode, languageTag)
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val fromName = hint.fromName
        if (fromName == null) {
            Text(
                text = stringResource(Res.string.transfer_hint_uncovered, amount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            val move = stringResource(Res.string.transfer_hint_move, amount, fromName, formatDate(suggestion.neededBy, languageTag))
            val note = when (hint.crossing) {
                BoundaryCrossing.Enters -> stringResource(Res.string.transfer_hint_enters)
                BoundaryCrossing.Leaves -> stringResource(Res.string.transfer_hint_leaves)
                null -> null
            }
            Text(
                text = buildAnnotatedString {
                    append(move)
                    if (note != null) {
                        append(" ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(note) }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (state.canRecord) {
                SecondaryButton(
                    text = stringResource(Res.string.transfer_action),
                    onClick = { onEvent(GroupSummaryEvent.TransferClicked(suggestion)) },
                    compact = true,
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
}

/** Horizontal bars of what each envelope spent this period (top 6); a bar opens the envelope, the link the Envelopes tab. */
@Composable
private fun SpendingSection(
    state: GroupSummaryUiState,
    onEvent: (GroupSummaryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FormSection(title = stringResource(Res.string.group_summary_spending_title), modifier = modifier) {
        SoftTile {
            val bar = LocalMoneyColors.current.outflow
            state.spending.forEach { item ->
                val name = if (item.envelope.kind == EnvelopeKind.System) stringResource(Res.string.envelopes_unassigned) else item.envelope.name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEvent(GroupSummaryEvent.SpendingClicked(item.envelope.id)) },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        MoneyText(
                            amountCents = -item.spentCents,
                            currencyCode = state.currencyCode,
                            role = MoneyRole.Flow,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                    }
                    // Length relative to the biggest bar: a display ratio
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.background)) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(state.spendingFraction(item)).clip(RoundedCornerShape(50)).background(bar))
                    }
                }
            }
            TextButton(onClick = { onEvent(GroupSummaryEvent.SeeAllEnvelopesClicked) }) {
                Text(stringResource(Res.string.group_summary_spending_all))
            }
        }
    }
}

@Composable
private fun FlowsSection(
    state: GroupSummaryUiState,
    languageTag: String,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.group_summary_flows_title)
    val zoom = rememberChartZoom(ZOOM_GROUP_FLOWS, title) {
        SoftTile { GroupFlowsChart(state, languageTag, ZoomedChartHeight) }
        ChartTable(
            columns = listOf(stringResource(Res.string.chart_legend_in), stringResource(Res.string.chart_legend_out)),
            rows = state.flows.asReversed().map {
                ChartTableRow(formatMonthShort(it.startDate, languageTag), listOf(it.inflowCents, -it.outflowCents))
            },
            currencyCode = state.currencyCode,
            role = MoneyRole.Flow,
        )
    }
    FormSection(title = title, modifier = modifier, action = { ChartZoomButton(zoom) }) {
        ZoomableChart(ZOOM_GROUP_FLOWS) { SoftTile { GroupFlowsChart(state, languageTag) } }
    }
}

@Composable
private fun GroupFlowsChart(state: GroupSummaryUiState, languageTag: String, height: Dp = FlowsChartHeight) {
    val groups = state.flows.map {
        ColumnGroup(formatMonthShort(it.startDate, languageTag), listOf(it.inflowCents, it.outflowCents))
    }
    val periods = state.flows.map {
        stringResource(
            Res.string.chart_flows_period,
            formatMonthShort(it.startDate, languageTag),
            formatMoney(it.inflowCents, state.currencyCode, languageTag),
            formatMoney(it.outflowCents, state.currencyCode, languageTag),
        )
    }.joinToString(" ")
    GroupedColumnChart(
        series = listOf(
            ColumnSeries(stringResource(Res.string.chart_legend_in), LocalMoneyColors.current.inflow),
            ColumnSeries(stringResource(Res.string.chart_legend_out), LocalMoneyColors.current.outflow),
        ),
        groups = groups,
        formatY = { compactAmount(it, languageTag) },
        formatValue = { formatMoney(it, state.currencyCode, languageTag) },
        contentDescription = stringResource(Res.string.chart_flows_description, state.flows.size, periods),
        height = height,
    )
}

private const val ZOOM_GROUP_FLOWS = "group-flows"
private const val ZOOM_GROUP_FREE = "group-free"

/** A row of small cards, one per due date until the end of the period; one muted card when there is none. */
@Composable
private fun UpcomingSection(state: GroupSummaryUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.detail_section_upcoming)) {
        if (state.upcoming.isEmpty()) {
            SoftTile {
                Text(
                    text = stringResource(Res.string.group_summary_upcoming_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.upcoming, key = { it.key }) { item -> UpcomingCard(item, state.currencyCode, languageTag) }
            }
        }
    }
}

@Composable
private fun UpcomingCard(item: DueOccurrence, currencyCode: String, languageTag: String) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatWeekdayDayMonth(item.dueDate, languageTag),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        MoneyText(
            amountCents = item.signedCents,
            currencyCode = currencyCode,
            role = MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}
