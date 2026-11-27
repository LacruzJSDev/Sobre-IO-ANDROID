package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.BoundaryCrossing
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeProgress
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.NEXT_DUE_WINDOW_DAYS
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.domain.model.TransferHint
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import dev.lacruzjs.sobreio.domain.model.shortfallDate
import dev.lacruzjs.sobreio.ui.undo.PendingUndoCommits
import dev.lacruzjs.sobreio.ui.loading.revalidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.abs

/**
 * The Group tab: the money really free today and at the end of the period with its projection, the next due dates with
 * the count of the ones to review (both handled in the review screen), the warnings, where the period's money went and the
 * money in and out by period. Every figure comes from the database; the app only joins names, signs due items and picks rows.
 */
class GroupSummaryViewModel(
    private val groupId: String,
    private val summary: GroupSummaryRepository,
    private val detail: AccountDetailRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupSummaryUiState())
    val state: StateFlow<GroupSummaryUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun onEvent(event: GroupSummaryEvent) {
        when (event) {
            is GroupSummaryEvent.GroupLoaded ->
                _state.update { it.copy(currencyCode = event.currencyCode, canRecord = event.canRecord) }
            GroupSummaryEvent.Refresh -> load()
            is GroupSummaryEvent.LookAheadSelected -> lookAhead(event.day)
            GroupSummaryEvent.LookAheadCleared -> _state.update { it.copy(lookAheadDay = null, lookAhead = null) }
            // Handled by the Route, which navigates
            GroupSummaryEvent.ReviewClicked,
            is GroupSummaryEvent.WarningClicked,
            is GroupSummaryEvent.SpendingClicked,
            GroupSummaryEvent.SeeAllEnvelopesClicked,
            is GroupSummaryEvent.TransferClicked,
            -> Unit
        }
    }

    init {
        // Painted from the cache before the first frame when the group was seen recently; read when it was not
        load()
    }

    private var lookAheadJob: Job? = null

    private fun lookAhead(day: LocalDate) {
        _state.update { it.copy(lookAheadDay = day, lookAhead = null) }
        lookAheadJob?.cancel()
        lookAheadJob = viewModelScope.launch {
            summary.getOutlookUntil(groupId, day)
                .onSuccess { figures -> _state.update { if (it.lookAheadDay == day) it.copy(lookAhead = figures) else it } }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // A delete made final as the previous screen closed is written before reading
            PendingUndoCommits.await()
            revalidate(onRefreshing = { refreshing -> _state.update { it.copy(isRefreshing = refreshing) } }) { read() }
        }
    }

    private suspend fun read() {
        val reads = coroutineScope {
            val todayD = async { calendar.getToday(groupId) }
            val outlookD = async { summary.getOutlook(groupId) }
            val pendingD = async { summary.getPending(groupId) }
            val accountsD = async { accounts.getAccounts(groupId) }
            val envelopesD = async { envelopes.getEnvelopes(groupId) }
            val progressD = async { envelopes.getProgress(groupId) }
            val shortfallsD = async { summary.getShortfalls(groupId) }
            val projectionD = async { summary.getFreeProjection(groupId) }
            val flowsD = async { summary.getPeriodFlows(groupId, FLOW_PERIODS) }
            val planD = async { summary.getTransferPlan(groupId) }
            Reads(
                todayD.await(), outlookD.await(), pendingD.await(), accountsD.await(), envelopesD.await(),
                progressD.await(), shortfallsD.await(), projectionD.await(), flowsD.await(), planD.await(),
            )
        }
        val failure = reads.today.exceptionOrNull() ?: reads.outlook.exceptionOrNull() ?: reads.pending.exceptionOrNull()
            ?: reads.accounts.exceptionOrNull() ?: reads.envelopes.exceptionOrNull()
        if (failure != null) {
            _state.update {
                if (it.outlook == null) it.copy(isLoading = false, error = failure.asAppError())
                else it.copy(isLoading = false, actionError = failure.asAppError())
            }
            return
        }
        val day = reads.today.getOrThrow()
        val outlook = reads.outlook.getOrThrow()
        val accountList = reads.accounts.getOrThrow()
        val envelopeList = reads.envelopes.getOrThrow()
        // Everything below is secondary: a failure leaves that card out instead of the whole tab
        val upcoming = summary.getUpcoming(groupId, day.plus(1, DateTimeUnit.DAY), day.plus(NEXT_DUE_WINDOW_DAYS, DateTimeUnit.DAY))
            .getOrDefault(emptyList())
            .filter { o -> o.isReviewable }
            .sortedWith(compareBy({ o -> o.dueDate }, { o -> o.name.lowercase() }))
            .take(GroupSummaryUiState.MAX_NEXT_MOVEMENTS)
        val spending = summary.getEnvelopeSpending(groupId, outlook.periodStart).getOrDefault(emptyList())
            .mapNotNull { row -> envelopeList.firstOrNull { it.id == row.envelopeId }?.let { SpendingBar(it, row.spentCents) } }
            .sortedWith(compareByDescending<SpendingBar> { it.spentCents }.thenBy { it.envelope.name.lowercase() })
            .take(GroupSummaryUiState.MAX_SPENDING_BARS)
        val warnings = buildWarnings(outlook, accountList, envelopeList, reads.progress.getOrDefault(emptyList()), reads.shortfalls.getOrDefault(emptyList()), reads.plan.getOrDefault(emptyList()))
        _state.update {
            it.copy(
                outlook = outlook,
                today = day,
                freeProjection = reads.projection.getOrDefault(emptyList()).sortedBy { p -> p.day },
                pending = reads.pending.getOrThrow().filter { o -> o.isReviewable },
                upcoming = upcoming,
                warnings = warnings,
                spending = spending,
                flows = reads.flows.getOrDefault(emptyList()),
                isLoading = false,
                error = null,
                actionError = null,
            )
        }
    }

    private suspend fun buildWarnings(
        outlook: GroupOutlook,
        accountList: List<Account>,
        envelopeList: List<Envelope>,
        progress: List<EnvelopeProgress>,
        shortfalls: List<AccountShortfall>,
        plan: List<TransferSuggestion>,
    ): List<GroupWarning> = buildList {
        progress
            .filter { it.state == ProgressState.Overspent }
            .mapNotNull { p -> envelopeList.firstOrNull { it.id == p.envelopeId }?.let { GroupWarning.EnvelopeOverspent(it.id, it.name, abs(p.currentCents)) } }
            .sortedBy { it.name.lowercase() }
            .let { addAll(it) }
        shortfalls.forEach { shortfall ->
            val account = accountList.firstOrNull { it.id == shortfall.accountId } ?: return@forEach
            val date = detail.getProjection(account.id).getOrNull()?.let { shortfallDate(it, shortfall.lowestCents) }
            val hints = plan.filter { it.toAccountId == account.id }.map { suggestion ->
                val from = suggestion.fromAccountId?.let { id -> accountList.firstOrNull { it.id == id } }
                TransferHint(
                    suggestion = suggestion,
                    fromName = from?.name,
                    crossing = when {
                        !suggestion.crossesBoundary || from == null -> null
                        account.inEnvelopes -> BoundaryCrossing.Enters
                        else -> BoundaryCrossing.Leaves
                    },
                )
            }
            add(GroupWarning.AccountShortfallOn(account.id, account.name, shortfall.lowestCents, date ?: outlook.periodEnd, hints))
        }
        if (outlook.committedShortfallCents > 0) add(GroupWarning.CommitmentsShortfall(outlook.committedShortfallCents))
    }

    private class Reads(
        val today: Result<LocalDate>,
        val outlook: Result<GroupOutlook>,
        val pending: Result<List<DueOccurrence>>,
        val accounts: Result<List<Account>>,
        val envelopes: Result<List<Envelope>>,
        val progress: Result<List<EnvelopeProgress>>,
        val shortfalls: Result<List<AccountShortfall>>,
        val projection: Result<List<dev.lacruzjs.sobreio.domain.model.FreePoint>>,
        val flows: Result<List<dev.lacruzjs.sobreio.domain.model.PeriodFlow>>,
        val plan: Result<List<TransferSuggestion>>,
    )

    private companion object {
        const val FLOW_PERIODS = 6
    }
}
