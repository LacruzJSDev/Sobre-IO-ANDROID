package dev.lacruzjs.sobreio.ui.commitmentdetail

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentOccurrence
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.OccurrenceStatus
import dev.lacruzjs.sobreio.ui.commitments.CommitmentSection
import dev.lacruzjs.sobreio.ui.commitments.sectionOf
import dev.lacruzjs.sobreio.ui.commitments.signedAmount
import dev.lacruzjs.sobreio.ui.loading.revalidate
import dev.lacruzjs.sobreio.ui.loading.stopIfCachePass
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

/**
 * Read only detail of one commitment as an account sees it: its resolved due items and its versions. Pending items are
 * not here, they are reviewed in the Group tab. Everything is read; nothing is computed beyond signs and names.
 */
class CommitmentDetailViewModel(
    private val groupId: String,
    private val accountId: String,
    private val commitmentId: String,
    private val commitments: CommitmentRepository,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommitmentDetailUiState())
    val state: StateFlow<CommitmentDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var hasLoaded = false

    init {
        // Painted from the cache before the first frame when the commitment was seen recently
        load(showSpinner = false)
    }

    fun onEvent(event: CommitmentDetailEvent) {
        when (event) {
            CommitmentDetailEvent.Refresh -> load(showSpinner = !hasLoaded)
            CommitmentDetailEvent.Retry -> load(showSpinner = true)
            CommitmentDetailEvent.LoadMoreOccurrences -> loadMoreOccurrences()
            // Handled by the Route
            CommitmentDetailEvent.BackClicked, CommitmentDetailEvent.EditClicked -> Unit
        }
    }

    private fun load(showSpinner: Boolean) {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            revalidate(onRefreshing = { refreshing -> _state.update { it.copy(isRefreshing = refreshing) } }) { read() }
        }
    }

    private suspend fun read() {
        run {
            val groupResult = groups.getGroup(groupId)
            val todayResult = calendar.getToday(groupId)
            val commitmentResult = commitments.getCommitment(commitmentId)
            val occurrencesResult = commitments.getOccurrences(commitmentId, offset = 0, limit = PAGE_SIZE)
            val failure = groupResult.exceptionOrNull() ?: todayResult.exceptionOrNull()
                ?: commitmentResult.exceptionOrNull() ?: occurrencesResult.exceptionOrNull()
            if (failure != null) {
                // A refresh that fails keeps what is on screen; only a first load turns into the error state
                if (!hasLoaded) _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
                return
            }
            val group = groupResult.getOrThrow()
            val today = todayResult.getOrThrow()
            val commitment = commitmentResult.getOrThrow()
            val version = commitment.versionFor(accountId, today) ?: commitment.currentVersion(today)
            if (version == null) {
                // A stale copy may lack the latest version: only the database says there is none
                stopIfCachePass()
                _state.update { it.copy(isLoading = false, error = AppError.NotFound) }
                return
            }
            // Names are optional: a row without one just leaves the line out
            val accountNames = accounts.getAccounts(groupId).getOrDefault(emptyList()).associate { it.id to it.name }
            val envelopeList = envelopes.getEnvelopes(groupId).getOrDefault(emptyList()).associateBy { it.id }

            // The reservation is a bonus: when it fails the block is simply left out
            val funding = if (version.funded && !commitment.isArchived) {
                commitments.getFundingStatus(commitment.id).getOrNull()
            } else {
                null
            }

            val section = sectionOf(version, accountId)
            // A contribution can only be changed with its link, which members of one group may not see
            val editableKind = when (section) {
                CommitmentSection.Expenses, CommitmentSection.Incomes, CommitmentSection.Transfers -> true
                CommitmentSection.Contributions -> commitments.getLinks(listOf(commitment.id))
                    .getOrDefault(emptyList()).any { it.outCommitmentId == commitment.id }
                else -> false
            }
            // Newest first; the one after it in the list is the previous version in time
            val versions = commitment.versions.sortedByDescending { it.effectiveFrom }
            val versionRows = versions.mapIndexed { index, v ->
                val previous = versions.getOrNull(index + 1)
                val envelope = if (v.kind == CommitmentKind.Expense) v.envelopeId?.let { envelopeList[it] } else null
                VersionRow(
                    version = v,
                    signedCents = signedAmount(v, accountId),
                    // effective_to is exclusive: the version ran until the day before
                    lastDay = v.effectiveTo?.minus(1, DateTimeUnit.DAY),
                    accountName = if (previous != null && previous.accountId != v.accountId) accountNames[v.accountId] else null,
                    envelopeName = envelope?.takeIf { it.kind != EnvelopeKind.System }?.name,
                    unassigned = envelope?.kind == EnvelopeKind.System,
                )
            }
            _state.update {
                it.copy(
                    name = commitment.name,
                    accountName = accountNames[version.accountId].orEmpty(),
                    version = version,
                    signedCents = signedAmount(version, accountId),
                    isArchived = commitment.isArchived,
                    funding = funding,
                    fundingEnvelopeName = funding?.envelopeId?.let { id -> envelopeList[id]?.takeIf { e -> e.kind != EnvelopeKind.System }?.name },
                    currencyCode = group.currencyCode,
                    canEdit = group.canEdit && !commitment.isArchived && editableKind,
                    occurrences = occurrencesResult.getOrThrow().items
                        .sortedByDescending { o -> o.dueDate }
                        .map { o -> o.toRow() },
                    occurrencesTotal = occurrencesResult.getOrThrow().total,
                    isLoadingMore = false,
                    loadMoreFailed = false,
                    versions = versionRows,
                    isLoading = false,
                    error = null,
                )
            }
            hasLoaded = true
        }
    }

    /** Appends the next page of the history. Ignored while one is loading or when everything is already loaded. */
    private fun loadMoreOccurrences() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMoreOccurrences) return
        _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
        loadMoreJob = viewModelScope.launch {
            val result = commitments.getOccurrences(commitmentId, offset = current.occurrences.size, limit = PAGE_SIZE)
            result.fold(
                onSuccess = { page ->
                    _state.update { state ->
                        // A due item resolved meanwhile can shift the pages: never show one twice
                        val known = state.occurrences.map { row -> row.dueDate }.toSet()
                        val added = page.items.filter { o -> o.dueDate !in known }.map { o -> o.toRow() }
                        state.copy(
                            occurrences = (state.occurrences + added).sortedByDescending { row -> row.dueDate },
                            occurrencesTotal = page.total,
                            isLoadingMore = false,
                            // An empty page with items still missing would loop: stop and offer the retry instead
                            loadMoreFailed = page.items.isEmpty() && state.occurrences.size < page.total,
                        )
                    }
                },
                onFailure = { _state.update { state -> state.copy(isLoadingMore = false, loadMoreFailed = true) } },
            )
        }
    }

    /** A transaction is already signed; a transfer is positive and leaves the account when this is its source. */
    private fun CommitmentOccurrence.toRow(): OccurrenceRow {
        val signed = amountCents?.let { cents ->
            if (fromAccountId != null && fromAccountId == accountId) -cents else cents
        }
        return OccurrenceRow(
            dueDate = dueDate,
            skipped = status == OccurrenceStatus.Skipped,
            signedCents = if (status == OccurrenceStatus.Skipped) null else signed,
            date = date?.takeIf { it != dueDate },
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
