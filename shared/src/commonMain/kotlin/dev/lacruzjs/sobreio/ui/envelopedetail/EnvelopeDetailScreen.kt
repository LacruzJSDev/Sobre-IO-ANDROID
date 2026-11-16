package dev.lacruzjs.sobreio.ui.envelopedetail

import androidx.compose.foundation.layout.BoxWithConstraints
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.action_see_all
import dev.lacruzjs.sobreio.generated.resources.chart_legend_assigned
import dev.lacruzjs.sobreio.generated.resources.chart_legend_spent
import dev.lacruzjs.sobreio.generated.resources.detail_no_movements
import dev.lacruzjs.sobreio.generated.resources.detail_section_latest
import dev.lacruzjs.sobreio.generated.resources.edit_envelope_title
import dev.lacruzjs.sobreio.generated.resources.envelope_chart_description
import dev.lacruzjs.sobreio.generated.resources.envelope_chart_period
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_activity
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_assigned
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_available
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_brought
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_committed
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_free
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_left
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_chart
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_commitments
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_free
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_period
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_transfers
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_shortfall
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_transfers_empty
import dev.lacruzjs.sobreio.generated.resources.envelope_funding_row
import dev.lacruzjs.sobreio.generated.resources.envelope_target_per_month
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.holder_common
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.generated.resources.transfer_from
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_commitment
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_commitment_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_cover
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_manual
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_release
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_release_plain
import dev.lacruzjs.sobreio.generated.resources.transfer_origin_target
import dev.lacruzjs.sobreio.generated.resources.transfer_to
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.CollapsibleListSection
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.charts.ColumnGroup
import dev.lacruzjs.sobreio.ui.components.charts.ColumnSeries
import dev.lacruzjs.sobreio.ui.components.charts.GroupedColumnChart
import dev.lacruzjs.sobreio.ui.components.charts.compactAmount
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatMonthShort
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 2 of the Envelopes tab, also for "Unassigned" (read only). Holds the lambdas of the back stack: [onEdit] opens the
 * envelope form, [onSeeAllMovements] the envelope's movements and [onOpenCommitment] a funding commitment's detail. It
 * reads again each time it is shown, so coming back from the form shows the new figures.
 */
@Composable
fun EnvelopeDetailRoute(
    groupId: String,
    envelopeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSeeAllMovements: () -> Unit,
    onOpenCommitment: (accountId: String, commitmentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvelopeDetailViewModel = koinViewModel { parametersOf(groupId, envelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(EnvelopeDetailEvent.Refresh) }
    EnvelopeDetailScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                EnvelopeDetailEvent.BackClicked -> onBack()
                EnvelopeDetailEvent.EditClicked -> onEdit()
                EnvelopeDetailEvent.SeeAllClicked -> onSeeAllMovements()
                is EnvelopeDetailEvent.CommitmentClicked -> onOpenCommitment(event.accountId, event.commitmentId)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun EnvelopeDetailScreen(
    state: EnvelopeDetailUiState,
    onEvent: (EnvelopeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val languageTag = Locale.current.toLanguageTag()
    val envelope = state.envelope
    // The system envelope is stored with a Spanish name: the UI has its own, translated
    val name = if (state.isUnassigned) stringResource(Res.string.envelopes_unassigned) else envelope?.name.orEmpty()
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = name,
                subtitle = state.groupName.takeIf { it.isNotEmpty() },
                onBack = { onEvent(EnvelopeDetailEvent.BackClicked) },
                leading = {
                    if (envelope != null) {
                        EntityAvatar(
                            name = name,
                            icon = if (state.isUnassigned) Icons.Filled.Inbox else iconFor(IconCategory.Envelope, envelope.icon),
                            color = EntityColor.fromHex(envelope.color),
                            size = 36.dp,
                        )
                    }
                },
                actions = {
                    if (state.canEditEnvelope) {
                        IconActionButton(
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.edit_envelope_title),
                            onClick = { onEvent(EnvelopeDetailEvent.EditClicked) },
                        )
                    }
                },
            )
            Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        state.error != null || envelope == null -> EmptyState(
                            message = stringResource((state.error ?: AppError.NotFound).messageRes()),
                        ) {
                            TextButton(onClick = { onEvent(EnvelopeDetailEvent.Retry) }) {
                                Text(stringResource(Res.string.action_retry))
                            }
                        }
                        else -> DetailContent(state, envelope, languageTag, onEvent)
                    }
                }
                // The amount of a deleted move is written here, where the language and the currency are known
                val undoMessage = state.undoMessage?.let { message ->
                    message.amountCents
                        ?.let { cents -> message.copy(argument = formatMoney(cents, state.currencyCode, languageTag)) }
                        ?: message
                }
                UndoSnackbarHost(
                    message = undoMessage,
                    onUndo = { onEvent(EnvelopeDetailEvent.UndoClicked) },
                    onDismiss = { onEvent(EnvelopeDetailEvent.UndoExpired) },
                )
            }
        }
    }
}

/** One scrolling column of titled sections whose content sits on soft tiles, with spacing and no dividers. */
@Composable
private fun DetailContent(
    state: EnvelopeDetailUiState,
    envelope: Envelope,
    languageTag: String,
    onEvent: (EnvelopeDetailEvent) -> Unit,
) {
    // The pane's own width decides the layout, not the window's
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val twoColumns = maxWidth >= ContentWidth.TwoColumnsMinPane
        val scroll = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .contentMaxWidth(if (twoColumns) ContentWidth.TwoColumns else ContentWidth.List)
            .padding(horizontal = 24.dp, vertical = 16.dp)
        val spacing = Arrangement.spacedBy(12.dp)
        if (twoColumns) {
            Column(modifier = scroll, verticalArrangement = spacing) {
                ErrorText(state.actionError, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // The envelope's figures on the left; its history and what funds it on the right
                    Column(modifier = Modifier.weight(1f), verticalArrangement = spacing) {
                        AvailableSection(envelope, state, languageTag)
                        state.period?.let { PeriodSection(it, state.currencyCode) }
                        state.outlook?.let { OutlookSection(it, state.currencyCode, languageTag) }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = spacing) {
                        if (state.chartPeriods.isNotEmpty()) ChartSection(state, languageTag)
                        if (state.funding.isNotEmpty()) FundingSection(state, languageTag, onEvent)
                        TransfersSection(state, languageTag, onEvent)
                        LatestMovementsSection(state, languageTag, onEvent)
                    }
                }
            }
        } else {
            Column(modifier = scroll, verticalArrangement = spacing) {
                ErrorText(state.actionError, modifier = Modifier.fillMaxWidth())
                AvailableSection(envelope, state, languageTag)
                state.period?.let { PeriodSection(it, state.currencyCode) }
                state.outlook?.let { OutlookSection(it, state.currencyCode, languageTag) }
                if (state.chartPeriods.isNotEmpty()) ChartSection(state, languageTag)
                if (state.funding.isNotEmpty()) FundingSection(state, languageTag, onEvent)
                TransfersSection(state, languageTag, onEvent)
                LatestMovementsSection(state, languageTag, onEvent)
            }
        }
    }
}

@Composable
private fun AvailableSection(envelope: Envelope, state: EnvelopeDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.envelope_detail_available)) {
        MoneyText(
            amountCents = envelope.balanceCents,
            currencyCode = state.currencyCode,
            role = MoneyRole.Balance,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val holder = if (state.hasSeveralMembers && !state.isUnassigned) {
            state.holderName ?: stringResource(Res.string.holder_common)
        } else {
            null
        }
        val target = if (envelope.monthlyTargetCents > 0) {
            stringResource(Res.string.envelope_target_per_month, formatMoney(envelope.monthlyTargetCents, state.currencyCode, languageTag))
        } else {
            null
        }
        val subtitle = listOfNotNull(
            if (state.isUnassigned) null else envelope.purpose?.let { stringResource(it.labelRes()) },
            holder,
            target,
        ).reduceOrNull { acc, part -> stringResource(Res.string.row_subtitle, acc, part) }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** What the current period did with the envelope, as `envelope_periods` says: nothing is summed here. */
@Composable
private fun PeriodSection(period: EnvelopePeriod, currencyCode: String) {
    StatCard(
        title = stringResource(Res.string.envelope_detail_section_period),
        currencyCode = currencyCode,
        rows = listOf(
            StatRow(stringResource(Res.string.envelope_detail_brought), period.openingCents, MoneyRole.Balance),
            StatRow(stringResource(Res.string.envelope_detail_assigned), period.assignedCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_activity), period.activityCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_left), period.closingCents, MoneyRole.Balance, emphasised = true),
        ),
    )
}

/** What is left once the commitments to the end of the period are paid (`envelope_outlook_current`). */
@Composable
private fun OutlookSection(outlook: EnvelopeOutlook, currencyCode: String, languageTag: String) {
    StatCard(
        title = stringResource(Res.string.envelope_detail_section_free),
        currencyCode = currencyCode,
        rows = listOf(
            StatRow(stringResource(Res.string.envelope_detail_committed), -outlook.committedCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_free), outlook.freeCents, MoneyRole.Balance, emphasised = true),
        ),
        warning = if (outlook.shortfallCents > 0) {
            stringResource(Res.string.envelope_detail_shortfall, formatMoney(outlook.shortfallCents, currencyCode, languageTag))
        } else {
            null
        },
    )
}

@Composable
private fun ChartSection(state: EnvelopeDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.envelope_detail_section_chart)) {
        SoftTile {
            val groups = state.chartPeriods.map { ColumnGroup(formatMonthShort(it.startDate, languageTag), it.chartColumns()) }
            val periods = state.chartPeriods.map {
                val (assigned, spent) = it.chartColumns()
                stringResource(
                    Res.string.envelope_chart_period,
                    formatMonthShort(it.startDate, languageTag),
                    formatMoney(assigned, state.currencyCode, languageTag),
                    formatMoney(spent, state.currencyCode, languageTag),
                )
            }.joinToString(" ")
            GroupedColumnChart(
                series = listOf(
                    ColumnSeries(stringResource(Res.string.chart_legend_assigned), LocalMoneyColors.current.inflow),
                    ColumnSeries(stringResource(Res.string.chart_legend_spent), LocalMoneyColors.current.outflow),
                ),
                groups = groups,
                formatY = { compactAmount(it, languageTag) },
                formatValue = { formatMoney(it, state.currencyCode, languageTag) },
                contentDescription = stringResource(Res.string.envelope_chart_description, state.chartPeriods.size, periods),
            )
        }
    }
}

/** The commitments that reserve money for the envelope, read only: tapping one opens its detail. */
@Composable
private fun FundingSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    FormSection(title = stringResource(Res.string.envelope_detail_section_commitments)) {
        state.funding.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onEvent(EnvelopeDetailEvent.CommitmentClicked(row.accountId, row.commitmentId)) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EntityAvatar(name = row.name, icon = row.kind.icon(), color = null)
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(text = row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = stringResource(
                            Res.string.envelope_funding_row,
                            formatMoney(row.thisPeriodCents, state.currencyCode, languageTag),
                            formatDate(row.nextDueDate, languageTag),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The live moves between envelopes, folded under a header with the exact count and paged 20 at a time. */
@Composable
private fun TransfersSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openId by remember { mutableStateOf<String?>(null) }
    CollapsibleListSection(
        title = stringResource(Res.string.envelope_detail_section_transfers),
        count = state.transfersTotal,
        items = state.transfers,
        emptyText = stringResource(Res.string.envelope_detail_transfers_empty),
        itemKey = { it.transfer.id },
        isLoadingMore = state.isLoadingMoreTransfers,
        loadMoreFailed = state.loadMoreTransfersFailed,
        onLoadMore = if (state.hasMoreTransfers) {
            { onEvent(EnvelopeDetailEvent.LoadMoreTransfers) }
        } else {
            null
        },
    ) { row ->
        // Only the manual ones can be deleted; the automatic ones are written by the database
        if (row.canDelete) {
            SwipeActionsRow(
                actions = listOf(deleteAction { onEvent(EnvelopeDetailEvent.DeleteTransferClicked(row.transfer.id)) }),
                isOpen = openId == row.transfer.id,
                // A row reporting "closed" must not close another one that has just opened
                onOpenChange = { open ->
                    if (open) openId = row.transfer.id else if (openId == row.transfer.id) openId = null
                },
                onClick = {},
            ) {
                TransferItem(row, state.currencyCode, languageTag)
            }
        } else {
            TransferItem(row, state.currencyCode, languageTag)
        }
    }
}

@Composable
private fun TransferItem(row: TransferRow, currencyCode: String, languageTag: String) {
    val transfer = row.transfer
    val label = when (transfer.origin) {
        TransferOrigin.Manual -> stringResource(Res.string.transfer_origin_manual)
        TransferOrigin.Target -> stringResource(Res.string.transfer_origin_target)
        TransferOrigin.Commitment -> transfer.commitmentName
            ?.let { stringResource(Res.string.transfer_origin_commitment, it) }
            ?: stringResource(Res.string.transfer_origin_commitment_plain)
        TransferOrigin.Cover -> stringResource(Res.string.transfer_origin_cover)
        TransferOrigin.Release -> transfer.commitmentName
            ?.let { stringResource(Res.string.transfer_origin_release, it) }
            ?: stringResource(Res.string.transfer_origin_release_plain)
    }
    val other = if (row.otherIsUnassigned) stringResource(Res.string.envelopes_unassigned) else row.otherName
    val direction = other?.let {
        stringResource(if (row.signedCents > 0) Res.string.transfer_from else Res.string.transfer_to, it)
    }
    val date = formatDate(row.date, languageTag)
    val second = if (direction != null) stringResource(Res.string.row_subtitle, direction, date) else date
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = second,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(
            amountCents = row.signedCents,
            currencyCode = currencyCode,
            role = MoneyRole.Flow,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun LatestMovementsSection(state: EnvelopeDetailUiState, languageTag: String, onEvent: (EnvelopeDetailEvent) -> Unit) {
    FormSection(title = stringResource(Res.string.detail_section_latest)) {
        SoftTile {
            if (state.latestMovements.isEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_no_movements),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.latestMovements.forEach { MovementItem(it, state, languageTag) }
            }
        }
        if (state.latestMovements.isNotEmpty()) {
            TextButton(onClick = { onEvent(EnvelopeDetailEvent.SeeAllClicked) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.action_see_all))
            }
        }
    }
}

@Composable
private fun MovementItem(movement: Movement, state: EnvelopeDetailUiState, languageTag: String) {
    val primary = movement.notes ?: stringResource(movement.kind.labelRes())
    val account = if (state.hasSeveralAccounts) state.accountNames[movement.accountId] else null
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (account != null) {
                Text(
                    text = account,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            MoneyText(
                amountCents = movement.amountCents,
                currencyCode = state.currencyCode,
                role = MoneyRole.Flow,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Text(
                text = formatDayMonth(movement.date, languageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val previewEnvelope = Envelope(
    "e1", "g", "Comida", EnvelopeKind.Spending, 40_000, 12_050, color = "#4F7A5A", icon = "groceries",
)

private val previewPeriods = listOf(
    EnvelopePeriod("1", LocalDate(2026, 9, 1), LocalDate(2026, 9, 30), 5_000, 40_000, -38_000, 7_000),
    EnvelopePeriod("2", LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), 7_000, 40_000, -41_000, 6_000),
    EnvelopePeriod("3", LocalDate(2026, 11, 1), LocalDate(2026, 11, 30), 6_000, 40_000, -33_950, 12_050),
)

@PreviewLightDark
@Composable
private fun EnvelopeDetailScreenPreview() {
    SobreioTheme {
        EnvelopeDetailScreen(
            state = EnvelopeDetailUiState(
                isLoading = false,
                groupName = "Casa",
                canEdit = true,
                envelope = previewEnvelope,
                period = previewPeriods.last(),
                chartPeriods = previewPeriods,
                outlook = EnvelopeOutlook(12_050, 20_000, -7_950, 7_950),
                funding = listOf(FundingRow("c", "a", "Seguro", CommitmentKind.Expense, 5_000, LocalDate(2027, 3, 19))),
                transfers = listOf(
                    TransferRow(
                        EnvelopeTransfer("t", "e0", "e1", 40_000, TransferOrigin.Target, Instant.parse("2026-11-01T08:00:00Z")),
                        40_000, null, true, LocalDate(2026, 11, 1),
                    ),
                ),
                transfersTotal = 1,
                latestMovements = listOf(
                    Movement(
                        "m", MovementSource.Transaction, "a", null, "e1", dev.lacruzjs.sobreio.domain.model.MovementKind.Expense,
                        -4_250, LocalDate(2026, 11, 13), "Supermercado", "u", "2026-11-13T10:00:00Z",
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}
