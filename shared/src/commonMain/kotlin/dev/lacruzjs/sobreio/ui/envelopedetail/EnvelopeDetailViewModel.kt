package dev.lacruzjs.sobreio.ui.envelopedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeDetailRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.data.repository.MovementRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.FundingCommitment
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.transfer_deleted
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * One envelope's detail: balance, the period tile, what is really free, the chart, the commitments that fund it, the moves
 * between envelopes and the latest movements. Every figure comes from the database through [EnvelopeDetailRepository] and
 * the existing repositories; this class only chooses what to ask, joins names and signs the moves for this envelope. The
 * system "Unassigned" envelope opens the same screen, read only.
 */
class EnvelopeDetailViewModel(
    private val groupId: String,
    private val envelopeId: String,
    private val groups: GroupRepository,
    private val envelopes: EnvelopeRepository,
    private val members: MemberRepository,
    private val accounts: AccountRepository,
    private val movements: MovementRepository,
    private val commitments: CommitmentRepository,
    private val detail: EnvelopeDetailRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeDetailUiState())
    val state: StateFlow<EnvelopeDetailUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var hasLoaded = false

    /** Moves already read (the hidden one included) and how many live ones the database has. */
    private var fetched = 0
    private var serverTotal = 0

    private var zone: TimeZone = TimeZone.UTC
    private var envelopeList: List<Envelope> = emptyList()

    fun onEvent(event: EnvelopeDetailEvent) {
        when (event) {
            EnvelopeDetailEvent.Refresh -> {
                // A delete still waiting for its snackbar is final before the screen is read again
                undo.commit()
                load(showSpinner = !hasLoaded)
            }
            EnvelopeDetailEvent.Retry -> load(showSpinner = true)
            EnvelopeDetailEvent.LoadMoreTransfers -> loadMoreTransfers()
            is EnvelopeDetailEvent.DeleteTransferClicked -> deleteTransfer(event.transferId)
            EnvelopeDetailEvent.UndoClicked -> undo.undo()
            EnvelopeDetailEvent.UndoExpired -> undo.commit()
            // Handled by the Route
            EnvelopeDetailEvent.BackClicked,
            EnvelopeDetailEvent.EditClicked,
            EnvelopeDetailEvent.SeeAllClicked,
            is EnvelopeDetailEvent.CommitmentClicked,
            is EnvelopeDetailEvent.EditTransferClicked,
            -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    // Loading ------------------------------------------------------------------------------------------------------

    private fun load(showSpinner: Boolean) {
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        loadJob?.cancel()
        loadMoreJob?.cancel()
        loadJob = viewModelScope.launch {
            val failure = loadAll()
            if (failure != null) {
                // A refresh that fails keeps what is on screen; only a first load turns into the error state
                if (!hasLoaded) _state.update { it.copy(isLoading = false, error = failure) }
                else _state.update { it.copy(actionError = failure) }
            }
        }
    }

    /** Reads everything; returns the failure that stops it, or null. */
    private suspend fun loadAll(): AppError? {
        val groupResult = groups.getGroup(groupId)
        val envelopeResult = detail.getEnvelope(envelopeId)
        (groupResult.exceptionOrNull() ?: envelopeResult.exceptionOrNull())?.let { return it.asAppError() }
        val group = groupResult.getOrThrow()
        val envelope = envelopeResult.getOrThrow()
        val isUnassigned = envelope.kind == EnvelopeKind.System
        // The outlook only covers live spending and saving envelopes
        val hasOutlook = !isUnassigned && !envelope.isArchived

        return coroutineScope {
            val periods = async { detail.getPeriods(envelopeId, CHART_PERIODS) }
            val outlook = async { if (hasOutlook) detail.getOutlook(groupId, envelopeId) else Result.success<EnvelopeOutlook?>(null) }
            val transfers = async { detail.getTransfers(envelopeId, 0, PAGE_SIZE) }
            val latest = async { movements.search(groupId, MovementFilter(envelopeId = envelopeId), LATEST_MOVEMENTS, 0, false) }
            val context = async { movements.getDateContext(groupId) }
            val fundingCandidates = async {
                if (isUnassigned) Result.success(emptyList<FundingCommitment>()) else detail.getFundingCommitments(envelopeId)
            }
            val memberList = async { members.getMembers(groupId) }
            val envelopeNames = async { envelopes.getEnvelopes(groupId) }
            val accountList = async { accounts.getAccounts(groupId) }

            val periodsResult = periods.await()
            val outlookResult = outlook.await()
            val transfersResult = transfers.await()
            val latestResult = latest.await()
            val contextResult = context.await()
            val candidatesResult = fundingCandidates.await()
            val failure = periodsResult.exceptionOrNull()
                ?: outlookResult.exceptionOrNull()
                ?: transfersResult.exceptionOrNull()
                ?: latestResult.exceptionOrNull()
                ?: contextResult.exceptionOrNull()
                ?: candidatesResult.exceptionOrNull()
            if (failure != null) return@coroutineScope failure.asAppError()

            // Which candidates reserve money in the current cycle is the database's to say
            val statuses = candidatesResult.getOrThrow()
                .map { candidate -> async { candidate to commitments.getFundingStatus(candidate.commitmentId) } }
                .awaitAll()
            statuses.firstNotNullOfOrNull { (_, status) -> status.exceptionOrNull() }?.let { return@coroutineScope it.asAppError() }
            val funding = statuses.mapNotNull { (candidate, status) ->
                status.getOrThrow()?.takeIf { it.envelopeId == envelopeId }?.let {
                    FundingRow(candidate.commitmentId, candidate.accountId, candidate.name, candidate.kind, it.thisPeriodCents, it.nextDueDate)
                }
            }

            // Only names to show: without them the rows simply lack a holder or a second line
            val people = memberList.await().getOrDefault(emptyList())
            envelopeList = envelopeNames.await().getOrDefault(emptyList())
            val accountRows = accountList.await().getOrDefault(emptyList())

            val today = contextResult.getOrThrow().today
            val periodRows = periodsResult.getOrThrow()
            zone = runCatching { TimeZone.of(group.timezone) }.getOrDefault(TimeZone.UTC)
            val page = transfersResult.getOrThrow()
            fetched = page.items.size
            serverTotal = page.total

            _state.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    actionError = null,
                    groupName = group.name,
                    currencyCode = group.currencyCode,
                    canEdit = group.canEdit,
                    canWriteTransfers = !group.isArchived && !group.isPendingDeletion,
                    envelope = envelope,
                    holderName = people.firstOrNull { m -> m.userId == envelope.ownerUserId }?.displayName,
                    hasSeveralMembers = people.hasSeveralMembers(),
                    period = periodRows.firstOrNull { p -> p.contains(today) } ?: periodRows.lastOrNull(),
                    chartPeriods = periodRows,
                    outlook = outlookResult.getOrThrow(),
                    funding = funding,
                    transfers = page.items.map { t -> t.toRow() },
                    transfersTotal = page.total,
                    hasMoreTransfers = fetched < serverTotal,
                    isLoadingMoreTransfers = false,
                    loadMoreTransfersFailed = false,
                    latestMovements = latestResult.getOrThrow().items,
                    accountNames = accountRows.associate { a -> a.id to a.name },
                    hasSeveralAccounts = accountRows.count { a -> !a.isArchived } > 1,
                )
            }
            hasLoaded = true
            null
        }
    }

    // Moves between envelopes ----------------------------------------------------------------------------------------

    private fun EnvelopeTransfer.toRow(): TransferRow {
        val incoming = toEnvelopeId == envelopeId
        val other = envelopeList.firstOrNull { it.id == if (incoming) fromEnvelopeId else toEnvelopeId }
        return TransferRow(
            transfer = this,
            signedCents = if (incoming) amountCents else -amountCents,
            otherName = other?.takeIf { it.kind != EnvelopeKind.System }?.name,
            otherIsUnassigned = other?.kind == EnvelopeKind.System,
            date = createdAt.toLocalDateTime(zone).date,
        )
    }

    private fun loadMoreTransfers() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMoreTransfers || !current.hasMoreTransfers) return
        _state.update { it.copy(isLoadingMoreTransfers = true, loadMoreTransfersFailed = false) }
        loadMoreJob = viewModelScope.launch {
            detail.getTransfers(envelopeId, fetched, PAGE_SIZE)
                .onSuccess { page ->
                    fetched += page.items.size
                    serverTotal = page.total
                    _state.update {
                        // An empty page ends the list even if the total said otherwise (rows voided meanwhile)
                        val more = page.items.isNotEmpty() && fetched < serverTotal
                        it.copy(
                            transfers = it.transfers + page.items.map { t -> t.toRow() },
                            hasMoreTransfers = more,
                            isLoadingMoreTransfers = false,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMoreTransfers = false, loadMoreTransfersFailed = true) } }
        }
    }

    /**
     * No confirmation: the move disappears at once and "Undo" follows. `voided_at` is written only when the snackbar ends
     * without Undo (or when something else needs it final). Only the origins of [TransferOrigin.canVoid] have the swipe.
     */
    private fun deleteTransfer(transferId: String) {
        val row = _state.value.transfers.firstOrNull { it.transfer.id == transferId && it.canDelete } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update {
            it.copy(transfers = it.transfers - row, transfersTotal = (it.transfersTotal - 1).coerceAtLeast(0), actionError = null)
        }
        undo.show(
            UndoableAction(
                message = Res.string.transfer_deleted,
                argument = "",
                amountCents = row.transfer.amountCents,
                commit = {
                    detail.voidTransfer(transferId).onSuccess {
                        // The database no longer has the row, so the next page starts one row earlier
                        fetched = (fetched - 1).coerceAtLeast(0)
                        serverTotal = (serverTotal - 1).coerceAtLeast(0)
                    }
                },
                undo = { Result.success(Unit) },
                restore = {
                    _state.update {
                        it.copy(
                            transfers = (it.transfers + row).sortedWith(
                                compareByDescending<TransferRow> { r -> r.transfer.createdAt }.thenBy { r -> r.transfer.id },
                            ),
                            transfersTotal = it.transfersTotal + 1,
                        )
                    }
                },
            ),
        )
    }

    companion object {
        const val CHART_PERIODS = 6
        const val PAGE_SIZE = 20
        const val LATEST_MOVEMENTS = 5
    }
}
