package dev.lacruzjs.sobreio.ui.groupsummary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.action_skip
import dev.lacruzjs.sobreio.generated.resources.detail_section_upcoming
import dev.lacruzjs.sobreio.generated.resources.group_summary_due_subtitle
import dev.lacruzjs.sobreio.generated.resources.group_summary_free_today
import dev.lacruzjs.sobreio.generated.resources.group_summary_period_range
import dev.lacruzjs.sobreio.generated.resources.group_summary_projected
import dev.lacruzjs.sobreio.generated.resources.group_summary_review_title
import dev.lacruzjs.sobreio.generated.resources.group_summary_upcoming_empty
import dev.lacruzjs.sobreio.generated.resources.group_summary_warnings_title
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.warning_account_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_commitments_shortfall
import dev.lacruzjs.sobreio.generated.resources.warning_envelope_overspent
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.CollapsibleListSection
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Group tab: the really free money, the due dates to review, the warnings and the next due dates, with the
 * undo snackbar and the confirm sheet. It lives inside the group shell, so the snackbar sits above the navigation bar.
 */
@Composable
fun GroupSummaryPane(
    state: GroupSummaryUiState,
    onEvent: (GroupSummaryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            val outlook = state.outlook
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && outlook == null -> EmptyState(message = stringResource(state.error.messageRes())) {
                    TextButton(onClick = { onEvent(GroupSummaryEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                outlook != null -> SummaryContent(state, outlook, onEvent)
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage,
            onUndo = { onEvent(GroupSummaryEvent.UndoClicked) },
            onDismiss = { onEvent(GroupSummaryEvent.UndoExpired) },
        )
    }
    ConfirmSheet(state, onEvent)
}

@Composable
private fun SummaryContent(state: GroupSummaryUiState, outlook: GroupOutlook, onEvent: (GroupSummaryEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(Res.string.group_summary_free_today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyText(
                amountCents = outlook.freeTodayCents,
                currencyCode = state.currencyCode,
                role = MoneyRole.Balance,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
            )
            val range = stringResource(
                Res.string.group_summary_period_range,
                formatDayMonth(outlook.periodStart, languageTag),
                formatDayMonth(outlook.periodEnd, languageTag),
            )
            Text(
                text = stringResource(
                    Res.string.group_summary_projected,
                    formatMoney(outlook.freeProjectedCents, state.currencyCode, languageTag),
                    range,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp))
        if (state.pending.isNotEmpty()) ReviewCard(state, languageTag, onEvent)
        if (state.warnings.isNotEmpty()) WarningsCard(state, languageTag, onEvent)
        CollapsibleListSection(
            title = stringResource(Res.string.detail_section_upcoming),
            count = state.upcoming.size,
            items = state.upcoming,
            emptyText = stringResource(Res.string.group_summary_upcoming_empty),
            itemKey = { it.key },
        ) { item -> UpcomingRow(item, state, languageTag) }
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReviewCard(state: GroupSummaryUiState, languageTag: String, onEvent: (GroupSummaryEvent) -> Unit) {
    SoftCard {
        CardTitle(stringResource(Res.string.group_summary_review_title, state.pendingCount))
        state.pending.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // No entity colour: a commitment is drawn in ink with the icon of its kind
                    EntityAvatar(name = item.name, icon = item.kind.icon(), color = null, size = 36.dp)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(
                                Res.string.group_summary_due_subtitle,
                                state.accountName(item.accountId),
                                formatDate(item.dueDate, languageTag),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MoneyText(
                        amountCents = item.signedCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Flow,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                if (state.canRecord) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(
                            text = stringResource(Res.string.action_skip),
                            onClick = { onEvent(GroupSummaryEvent.SkipClicked(item.key)) },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                        PrimaryButton(
                            text = stringResource(Res.string.action_confirm),
                            onClick = { onEvent(GroupSummaryEvent.ConfirmClicked(item.key)) },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
            }
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
        }
    }
}

@Composable
private fun UpcomingRow(item: DueOccurrence, state: GroupSummaryUiState, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.row_subtitle, formatDate(item.dueDate, languageTag), state.accountName(item.accountId)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(
            amountCents = item.signedCents,
            currencyCode = state.currencyCode,
            role = MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
