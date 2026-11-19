package dev.lacruzjs.sobreio.ui.groupsummary

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_settings_title
import dev.lacruzjs.sobreio.generated.resources.ic_settings
import dev.lacruzjs.sobreio.ui.components.LabelTooltip
import dev.lacruzjs.sobreio.ui.components.TabHeader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Group tab of the group shell: the summary, with the settings gear in the shell's [header]. Its ViewModel is the
 * same instance the shell reads for the tab's badge (both are kept by the group's entry); the shell also refreshes it,
 * since the badge counts whatever tab is open.
 */
@Composable
fun GroupSummaryTabRoute(
    group: Group,
    header: TabHeader,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenAccount: (accountId: String) -> Unit,
    onOpenEnvelope: (envelopeId: String) -> Unit,
    onShowEnvelopes: () -> Unit,
    onOpenTransferForm: (TransferSuggestion) -> Unit,
    viewModel: GroupSummaryViewModel = koinViewModel { parametersOf(group.id) },
) {
    // Any member may review a due date, but not in an archived group or one pending deletion; sent before the first frame
    // of the tab (a plain state update, safe to repeat)
    remember(viewModel, group) {
        viewModel.onEvent(GroupSummaryEvent.GroupLoaded(group.currencyCode, !group.isArchived && !group.isPendingDeletion))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    header(state.isRefreshing) {
        LabelTooltip(stringResource(Res.string.group_settings_title)) {
            IconButton(onClick = onOpenSettings) {
                Icon(painter = painterResource(Res.drawable.ic_settings), contentDescription = stringResource(Res.string.group_settings_title))
            }
        }
    }
    GroupSummaryPane(
        state = state,
        onEvent = { event ->
            when (event) {
                is GroupSummaryEvent.WarningClicked -> when (val warning = event.warning) {
                    is GroupWarning.EnvelopeOverspent -> onOpenEnvelope(warning.envelopeId)
                    is GroupWarning.AccountShortfallOn -> onOpenAccount(warning.accountId)
                    is GroupWarning.CommitmentsShortfall -> onShowEnvelopes()
                }
                is GroupSummaryEvent.TransferClicked -> onOpenTransferForm(event.suggestion)
                GroupSummaryEvent.ReviewClicked -> onOpenReview()
                is GroupSummaryEvent.SpendingClicked -> onOpenEnvelope(event.envelopeId)
                GroupSummaryEvent.SeeAllEnvelopesClicked -> onShowEnvelopes()
                else -> viewModel.onEvent(event)
            }
        },
    )
}
