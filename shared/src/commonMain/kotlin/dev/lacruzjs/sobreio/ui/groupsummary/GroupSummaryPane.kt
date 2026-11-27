package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.generated.resources.balance_on_day_choose
import dev.lacruzjs.sobreio.generated.resources.outlook_until_label
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_free_on
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_note
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_targets
import dev.lacruzjs.sobreio.generated.resources.group_look_ahead_title
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.tileShape
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_commitments
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_contributions
import dev.lacruzjs.sobreio.generated.resources.group_hero_note_income
import dev.lacruzjs.sobreio.generated.resources.group_summary_free_today
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ListSkeleton
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.TwoColumnLayout
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Group tab: the hero card with the free money and its projection, the next movements (whose icon opens the
 * review), the warnings, where the period's money went and the money in and out by period. It lives inside the group shell.
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
    // What needs attention now on the left (the free money, the next movements with what to review, the warnings); how
    // the period went on the right. Each column scrolls by itself on a wide pane.
    TwoColumnLayout(
        top = { ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp)) },
        left = {
            GroupHero(state, outlook, languageTag)
            NextMovementsSection(state, languageTag, onEvent)
            if (state.warnings.isNotEmpty()) WarningsCard(state, languageTag, onEvent)
            LookAheadSection(state, languageTag, onEvent)
        },
        right = {
            if (state.spending.isNotEmpty()) SpendingSection(state, onEvent)
            if (state.flows.isNotEmpty()) FlowsSection(state, languageTag)
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
                    if (figures.pendingTargetsCents > 0) {
                        add(StatRow(stringResource(Res.string.group_look_ahead_targets), -figures.pendingTargetsCents, MoneyRole.Flow))
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

@Composable
internal fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(tileShape())
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
internal fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
