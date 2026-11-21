package dev.lacruzjs.sobreio.ui.envelopedetail

import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import dev.lacruzjs.sobreio.generated.resources.info_envelope_period_2
import dev.lacruzjs.sobreio.generated.resources.info_envelope_period_1
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_left
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_activity
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
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
import dev.lacruzjs.sobreio.generated.resources.edit_envelope_title
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_activity
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_assigned
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_available
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_brought
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_left
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_period
import dev.lacruzjs.sobreio.generated.resources.envelope_target_per_month
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.holder_common
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.DetailSkeleton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.TwoColumnLayout
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.envelopes.labelRes
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 2 of the Envelopes tab, also for "Unassigned". Read only: nothing is edited or deleted here. Holds the lambdas of
 * the back stack: [onEdit] opens the envelope form, [onSeeAllMovements] the envelope's movements, [onOpenTransfers] its
 * moves between envelopes (where they are managed) and [onOpenCommitment] a funding commitment's detail. It reads again
 * each time it is shown, so coming back from any of them shows the new figures.
 */
@Composable
fun EnvelopeDetailRoute(
    groupId: String,
    envelopeId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSeeAllMovements: () -> Unit,
    onOpenTransfers: () -> Unit,
    onOpenCommitment: (accountId: String, commitmentId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EnvelopeDetailViewModel = koinViewModel { parametersOf(groupId, envelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(EnvelopeDetailEvent.Refresh) }
    // Changes made elsewhere in the group are read as they happen (D-55)
    RefreshOnRemoteChange(paused = false) { viewModel.onEvent(EnvelopeDetailEvent.Refresh) }
    EnvelopeDetailScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                EnvelopeDetailEvent.BackClicked -> onBack()
                EnvelopeDetailEvent.EditClicked -> onEdit()
                EnvelopeDetailEvent.SeeAllClicked -> onSeeAllMovements()
                EnvelopeDetailEvent.TransfersClicked -> onOpenTransfers()
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
                isRefreshing = state.isRefreshing,
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
                        state.isLoading -> DetailSkeleton()
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
    // The envelope's figures and their history on the left; what moves money in and out on the right, each with its
    // own scroll on a wide pane
    TwoColumnLayout(
        spacing = 12.dp,
        top = { ErrorText(state.actionError, modifier = Modifier.fillMaxWidth()) },
        left = {
            AvailableSection(envelope, state, languageTag)
            // Without an outlook (Unassigned, archived) there is no card to open, so "Balance on" stays here
            if (state.outlook == null) EnvelopeBalanceOnDay(state, onEvent)
            state.period?.let { PeriodSection(it, state.currencyCode) }
            state.outlook?.let { periodOutlook -> ZoomableOutlook(state, periodOutlook, languageTag, onEvent) }
            if (state.chartPeriods.isNotEmpty()) ChartSection(state, languageTag)
        },
        right = {
            if (state.funding.isNotEmpty()) FundingSection(state, languageTag, onEvent)
            TransfersSection(state, languageTag, onEvent)
            LatestMovementsSection(state, languageTag, onEvent)
        },
    )
}

@Composable
private fun AvailableSection(envelope: Envelope, state: EnvelopeDetailUiState, languageTag: String) {
    FormSection(title = stringResource(Res.string.envelope_detail_available), segmented = false) {
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
        info = listOf(Res.string.info_envelope_period_1, Res.string.info_envelope_period_2),
        currencyCode = currencyCode,
        rows = listOf(
            StatRow(stringResource(Res.string.envelope_detail_brought), period.openingCents, MoneyRole.Balance),
            StatRow(stringResource(Res.string.envelope_detail_assigned), period.assignedCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_activity), period.activityCents, MoneyRole.Flow),
            StatRow(stringResource(Res.string.envelope_detail_left), period.closingCents, MoneyRole.Balance, emphasised = true),
        ),
    )
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
