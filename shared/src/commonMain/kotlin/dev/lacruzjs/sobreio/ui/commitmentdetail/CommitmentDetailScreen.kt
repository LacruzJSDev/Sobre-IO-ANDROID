package dev.lacruzjs.sobreio.ui.commitmentdetail

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.commitment_detail_changes_title
import dev.lacruzjs.sobreio.generated.resources.commitment_detail_history_empty
import dev.lacruzjs.sobreio.generated.resources.commitment_detail_history_title
import dev.lacruzjs.sobreio.generated.resources.commitment_due_on
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_due_now
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_none_this_period
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_progress
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_this_period
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_this_period_no_envelope
import dev.lacruzjs.sobreio.generated.resources.commitment_funding_title
import dev.lacruzjs.sobreio.generated.resources.commitment_registered_on
import dev.lacruzjs.sobreio.generated.resources.commitment_skipped
import dev.lacruzjs.sobreio.generated.resources.commitment_version_account
import dev.lacruzjs.sobreio.generated.resources.commitment_version_envelope
import dev.lacruzjs.sobreio.generated.resources.commitment_version_open
import dev.lacruzjs.sobreio.generated.resources.commitment_version_range
import dev.lacruzjs.sobreio.generated.resources.edit_commitment_title
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.group_archived_label
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.commitments.frequencyText
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.CollapsibleListSection
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.FormInfoTile
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatSignedMoney
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 3 of the Accounts tab, read only. Holds the lambdas of the back stack: [onEdit] opens the commitment form. It
 * reads again each time it is shown, so coming back from the form shows the new versions.
 */
@Composable
fun CommitmentDetailRoute(
    groupId: String,
    accountId: String,
    commitmentId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CommitmentDetailViewModel = koinViewModel { parametersOf(groupId, accountId, commitmentId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(CommitmentDetailEvent.Refresh) }
    CommitmentDetailScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                CommitmentDetailEvent.BackClicked -> onBack()
                CommitmentDetailEvent.EditClicked -> onEdit()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CommitmentDetailScreen(
    state: CommitmentDetailUiState,
    onEvent: (CommitmentDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val version = state.version
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = state.name,
                subtitle = state.accountName.takeIf { it.isNotEmpty() },
                onBack = { onEvent(CommitmentDetailEvent.BackClicked) },
                leading = {
                    if (version != null) EntityAvatar(name = state.name, icon = version.kind.icon(), color = null, size = 36.dp)
                },
                actions = {
                    if (state.canEdit) {
                        IconActionButton(
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.edit_commitment_title),
                            onClick = { onEvent(CommitmentDetailEvent.EditClicked) },
                        )
                    }
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null || version == null -> EmptyState(
                        message = stringResource((state.error ?: AppError.NotFound).messageRes()),
                    ) {
                        TextButton(onClick = { onEvent(CommitmentDetailEvent.Retry) }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                    else -> DetailContent(state, version, onEvent)
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: CommitmentDetailUiState,
    version: CommitmentVersion,
    onEvent: (CommitmentDetailEvent) -> Unit,
) {
    val languageTag = Locale.current.toLanguageTag()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val rest = listOfNotNull(
            frequencyText(version, languageTag),
            if (state.isArchived) stringResource(Res.string.group_archived_label) else null,
        ).reduceOrNull { acc, part -> stringResource(Res.string.row_subtitle, acc, part) }
        Row(modifier = Modifier.padding(horizontal = 4.dp)) {
            MoneyText(
                amountCents = state.signedCents,
                currencyCode = state.currencyCode,
                role = MoneyRole.Flow,
                style = MaterialTheme.typography.titleMedium,
            )
            // The separator comes from the same "a · b" resource, with nothing before it
            if (rest != null) {
                Text(
                    text = stringResource(Res.string.row_subtitle, "", rest),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        state.funding?.let { FundingBlock(it, state.fundingEnvelopeName, state.currencyCode, languageTag) }

        CollapsibleListSection(
            title = stringResource(Res.string.commitment_detail_history_title),
            count = state.occurrencesTotal,
            items = state.occurrences,
            initiallyExpanded = true,
            emptyText = stringResource(Res.string.commitment_detail_history_empty),
            itemKey = { it.dueDate.toString() },
            isLoadingMore = state.isLoadingMore,
            loadMoreFailed = state.loadMoreFailed,
            onLoadMore = if (state.hasMoreOccurrences) {
                { onEvent(CommitmentDetailEvent.LoadMoreOccurrences) }
            } else {
                null
            },
        ) { OccurrenceItem(it, state.currencyCode, languageTag) }

        CollapsibleListSection(
            title = stringResource(Res.string.commitment_detail_changes_title),
            count = state.versions.size,
            items = state.versions,
            // One version is nothing to hide; a long story starts folded
            initiallyExpanded = state.versions.size == 1,
            itemKey = { it.version.id },
        ) { VersionItem(it, state.currencyCode, languageTag) }
    }
}

/** The reservation of a funded commitment: what this period sets aside and how far the cycle has got. */
@Composable
private fun FundingBlock(funding: CommitmentFunding, envelopeName: String?, currencyCode: String, languageTag: String) {
    val thisPeriod = formatMoney(funding.thisPeriodCents, currencyCode, languageTag)
    val reserved = formatMoney(funding.reservedInCycleCents, currencyCode, languageTag)
    val total = formatMoney(funding.cycleAmountCents, currencyCode, languageTag)
    val due = formatDate(funding.nextDueDate, languageTag)
    FormInfoTile(
        title = stringResource(Res.string.commitment_funding_title),
        text = when {
            funding.thisPeriodCents <= 0 -> stringResource(Res.string.commitment_funding_none_this_period)
            envelopeName != null -> stringResource(Res.string.commitment_funding_this_period, thisPeriod, envelopeName)
            else -> stringResource(Res.string.commitment_funding_this_period_no_envelope, thisPeriod)
        },
        secondary = if (funding.periodsLeft <= 0) {
            stringResource(Res.string.commitment_funding_due_now, reserved, total, due)
        } else {
            pluralStringResource(Res.plurals.commitment_funding_progress, funding.periodsLeft, reserved, total, due, funding.periodsLeft)
        },
    )
}

@Composable
private fun OccurrenceItem(row: OccurrenceRow, currencyCode: String, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(Res.string.commitment_due_on, formatDate(row.dueDate, languageTag)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            if (row.skipped || row.signedCents == null) {
                Text(
                    text = stringResource(Res.string.commitment_skipped),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                MoneyText(
                    amountCents = row.signedCents,
                    currencyCode = currencyCode,
                    role = MoneyRole.Flow,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                )
                if (row.date != null) {
                    Text(
                        text = stringResource(Res.string.commitment_registered_on, formatDate(row.date, languageTag)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionItem(row: VersionRow, currencyCode: String, languageTag: String) {
    val version = row.version
    val range = if (row.lastDay == null) {
        stringResource(Res.string.commitment_version_open, formatDate(version.effectiveFrom, languageTag))
    } else {
        stringResource(
            Res.string.commitment_version_range,
            formatDate(version.effectiveFrom, languageTag),
            formatDate(row.lastDay, languageTag),
        )
    }
    val details = listOfNotNull(
        frequencyText(version, languageTag),
        row.accountName?.let { stringResource(Res.string.commitment_version_account, it) },
        when {
            row.unassigned -> stringResource(Res.string.commitment_version_envelope, stringResource(Res.string.envelopes_unassigned))
            row.envelopeName != null -> stringResource(Res.string.commitment_version_envelope, row.envelopeName)
            else -> null
        },
    ).reduce { acc, part -> stringResource(Res.string.row_subtitle, acc, part) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = range, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            MoneyText(
                amountCents = row.signedCents,
                currencyCode = currencyCode,
                role = MoneyRole.Flow,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val previewVersion = CommitmentVersion(
    id = "v", commitmentId = "c", effectiveFrom = LocalDate(2026, 1, 1), effectiveTo = null, kind = CommitmentKind.Expense,
    amountCents = 69_815, accountId = "a", firstDueDate = LocalDate(2026, 1, 5), isRecurring = true, everyN = 1,
    unit = FrequencyUnit.Month, anchorDay = 5,
)

@PreviewLightDark
@Composable
private fun CommitmentDetailPreview() {
    SobreioTheme {
        CommitmentDetailScreen(
            state = CommitmentDetailUiState(
                name = "Alquiler",
                accountName = "Cuenta corriente",
                version = previewVersion,
                signedCents = -69_815,
                canEdit = true,
                funding = CommitmentFunding(
                    envelopeId = "e", nextDueDate = LocalDate(2027, 3, 19), cycleAmountCents = 25_100, cycleStart = LocalDate(2026, 11, 1),
                    reservedInCycleCents = 5_020, thisPeriodCents = 5_020, periodsInCycle = 5, periodsLeft = 4,
                ),
                fundingEnvelopeName = "Seguros",
                occurrencesTotal = 2,
                occurrences = listOf(
                    OccurrenceRow(LocalDate(2026, 11, 5), skipped = false, signedCents = -69_815, date = LocalDate(2026, 11, 7)),
                    OccurrenceRow(LocalDate(2026, 10, 5), skipped = true, signedCents = null, date = null),
                ),
                versions = listOf(
                    VersionRow(previewVersion, -69_815, lastDay = null, accountName = "Cuenta corriente", unassigned = true),
                ),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
