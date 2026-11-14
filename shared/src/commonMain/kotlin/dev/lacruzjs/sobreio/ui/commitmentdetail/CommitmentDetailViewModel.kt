package dev.lacruzjs.sobreio.ui.commitmentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
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
    private val detail: AccountDetailRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommitmentDetailUiState())
    val state: StateFlow<CommitmentDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var hasLoaded = false

    fun onEvent(event: CommitmentDetailEvent) {
        when (event) {
            CommitmentDetailEvent.Refresh -> load(showSpinner = !hasLoaded)
            CommitmentDetailEvent.Retry -> load(showSpinner = true)
            // Handled by the Route
            CommitmentDetailEvent.BackClicked, CommitmentDetailEvent.EditClicked -> Unit
        }
    }

    private fun load(showSpinner: Boolean) {
        loadJob?.cancel()
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            val groupResult = groups.getGroup(groupId)
            val todayResult = detail.getToday(groupId)
            val commitmentResult = commitments.getCommitment(commitmentId)
            val occurrencesResult = commitments.getOccurrences(commitmentId)
            val failure = groupResult.exceptionOrNull() ?: todayResult.exceptionOrNull()
                ?: commitmentResult.exceptionOrNull() ?: occurrencesResult.exceptionOrNull()
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
                return@launch
            }
            val group = groupResult.getOrThrow()
            val today = todayResult.getOrThrow()
            val commitment = commitmentResult.getOrThrow()
            val version = commitment.versionFor(accountId, today) ?: commitment.currentVersion(today)
            if (version == null) {
                _state.update { it.copy(isLoading = false, error = AppError.NotFound) }
                return@launch
            }
            // Names are optional: a row without one just leaves the line out
            val accountNames = accounts.getAccounts(groupId).getOrDefault(emptyList()).associate { it.id to it.name }
            val envelopeList = envelopes.getEnvelopes(groupId).getOrDefault(emptyList()).associateBy { it.id }

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
                    currencyCode = group.currencyCode,
                    canEdit = group.canEdit && !commitment.isArchived && editableKind,
                    occurrences = occurrencesResult.getOrThrow()
                        .sortedByDescending { o -> o.dueDate }
                        .map { o -> o.toRow() },
                    versions = versionRows,
                    isLoading = false,
                    error = null,
                )
            }
            hasLoaded = true
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
}
