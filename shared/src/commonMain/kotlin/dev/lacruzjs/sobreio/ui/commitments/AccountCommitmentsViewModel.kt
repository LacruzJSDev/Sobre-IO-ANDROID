package dev.lacruzjs.sobreio.ui.commitments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.commitment_deleted
import dev.lacruzjs.sobreio.generated.resources.commitment_reopened
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import dev.lacruzjs.sobreio.ui.loading.revalidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.time.Clock

/**
 * The Commitments tab of one account: the commitments whose current version touches it, in sections. Deleting archives at
 * once and offers Undo (same as the account list); a contribution to another group archives both halves through its link.
 */
class AccountCommitmentsViewModel(
    private val groupId: String,
    private val accountId: String,
    private val commitments: CommitmentRepository,
    private val groups: GroupRepository,
    private val detail: AccountDetailRepository,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountCommitmentsUiState())
    val state: StateFlow<AccountCommitmentsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var loadJob: Job? = null

    fun onEvent(event: CommitmentsEvent) {
        when (event) {
            CommitmentsEvent.Refresh -> {
                // A delete still waiting for its snackbar must be final before the list is read again
                undo.commit()
                load()
            }
            is CommitmentsEvent.DeleteClicked -> delete(event.commitmentId)
            is CommitmentsEvent.ReopenClicked -> reopen(event.commitmentId)
            CommitmentsEvent.UndoClicked -> undo.undo()
            CommitmentsEvent.UndoExpired -> undo.commit()
            // Handled by the Route, which opens the form or the detail
            is CommitmentsEvent.EditClicked, is CommitmentsEvent.OpenClicked -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    init {
        // Painted from the cache before the first frame when the account was seen recently
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            revalidate(onRefreshing = { refreshing -> _state.update { it.copy(isRefreshing = refreshing) } }) { read() }
        }
    }

    private suspend fun read() {
        run {
            val groupResult = groups.getGroup(groupId)
            val todayResult = detail.getToday(groupId)
            val listResult = commitments.getAccountCommitments(accountId)
            val failure = groupResult.exceptionOrNull() ?: todayResult.exceptionOrNull() ?: listResult.exceptionOrNull()
            if (failure != null) {
                _state.update {
                    if (it.isLoading) it.copy(isLoading = false, error = failure.asAppError()) else it.copy(actionError = failure.asAppError())
                }
                return
            }
            val group = groupResult.getOrThrow()
            val today = todayResult.getOrThrow()
            // Names of the accounts a commitment moves to; optional, a row without one just omits the "moves to" part
            val accountNames = accounts.getAccounts(groupId).getOrDefault(emptyList()).associate { it.id to it.name }
            // What this account sees: the version in force when it touches the account, else the first future one that does
            // (a commitment moving here). An old version of a commitment that already left is not listed.
            val mine = listResult.getOrThrow().mapNotNull { c -> c.versionFor(accountId, today)?.let { c to it } }
            // Both are optional: without links contributions are read only, without dates the rows show no "Next"
            val links = commitments.getLinks(mine.filter { (_, v) -> v.kind == CommitmentKind.GroupTransferOut }.map { it.first.id })
                .getOrDefault(emptyList())
                .associateBy { it.outCommitmentId }
            val nextDue = detail.getUpcoming(groupId, accountId, today, today.plus(NEXT_DUE_DAYS, DateTimeUnit.DAY))
                .getOrDefault(emptyList())
                .groupBy { it.commitmentId }
                .mapValues { (_, items) -> items.minOf { it.dueDate } }

            // Registered this period: only to say "Paid this period"; without it the row shows its next date as before
            val period = detail.getCurrentPeriod(groupId).getOrNull()
            val paid = if (period == null) emptySet() else commitments
                .getPaidCommitmentIds(mine.filter { (c, _) -> !c.isArchived }.map { it.first.id }, period.start, period.end)
                .getOrDefault(emptySet())

            val all = mine.map { (c, v) ->
                val section = sectionOf(v, accountId)
                val link = if (section == CommitmentSection.Contributions) links[c.id] else null
                val arriving = v.effectiveFrom > today
                // In force here today but the next version goes elsewhere: say where and when
                val leavesTo = if (arriving) null else c.nextVersion(today)?.takeIf { !it.touches(accountId) }
                CommitmentRow(
                    commitment = c,
                    version = v,
                    section = section,
                    signedCents = signedAmount(v, accountId),
                    nextDue = if (c.isArchived) null else nextDue[c.id],
                    paidThisPeriod = c.id in paid,
                    link = link,
                    startsOn = v.effectiveFrom.takeIf { arriving },
                    movesToName = leavesTo?.let { accountNames[it.accountId] },
                    movesOn = leavesTo?.effectiveFrom,
                    editable = group.canEdit && when (section) {
                        CommitmentSection.Expenses, CommitmentSection.Incomes, CommitmentSection.Transfers -> true
                        CommitmentSection.Contributions -> link != null
                        else -> false
                    },
                )
            }
            val (archived, live) = all.partition { it.commitment.isArchived }
            _state.update {
                it.copy(
                    rows = live.sortedByName(),
                    archivedRows = archived.sortedByName(),
                    currencyCode = group.currencyCode,
                    canEdit = group.canEdit,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    private fun List<CommitmentRow>.sortedByName() = sortedBy { it.commitment.name.lowercase() }

    private suspend fun archive(row: CommitmentRow): Result<Unit> = row.link
        ?.let { commitments.archiveLinkedCommitment(it.id) }
        ?: commitments.archiveCommitment(row.id)

    private suspend fun unarchive(row: CommitmentRow): Result<Unit> = row.link
        ?.let { commitments.unarchiveLinkedCommitment(it) }
        ?: commitments.unarchiveCommitment(row.id)

    /** No confirmation: the row goes to "Archived" at once and the snackbar's "Undo" unarchives it. */
    private fun delete(commitmentId: String) {
        val current = _state.value
        val row = current.rows.firstOrNull { it.id == commitmentId }?.takeIf { it.editable } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        val archived = row.copy(
            commitment = row.commitment.copy(archivedAt = Clock.System.now().toString()),
            nextDue = null,
        )
        _state.update {
            it.copy(
                rows = it.rows - row,
                archivedRows = (it.archivedRows + archived).sortedByName(),
                actionError = null,
            )
        }
        val restore = {
            _state.update {
                it.copy(rows = (it.rows + row).sortedByName(), archivedRows = it.archivedRows - archived)
            }
            load()
        }
        viewModelScope.launch {
            archive(row)
                .onSuccess {
                    undo.show(
                        UndoableAction(
                            message = Res.string.commitment_deleted,
                            argument = row.commitment.name,
                            commit = { Result.success(Unit) },
                            undo = { unarchive(row) },
                            restore = restore,
                        ),
                    )
                }
                .onFailure { e ->
                    restore()
                    _state.update { it.copy(actionError = e.asAppError()) }
                }
        }
    }

    /** An archived commitment becomes live again and moves to the main list; "Undo" archives it again. */
    private fun reopen(commitmentId: String) {
        val current = _state.value
        val row = current.archivedRows.firstOrNull { it.id == commitmentId }?.takeIf { it.editable } ?: return
        undo.commit()
        _state.update { it.copy(actionError = null) }
        viewModelScope.launch {
            unarchive(row)
                .onSuccess {
                    _state.update {
                        it.copy(
                            rows = (it.rows + row.copy(commitment = row.commitment.copy(archivedAt = null))).sortedByName(),
                            archivedRows = it.archivedRows - row,
                        )
                    }
                    undo.show(
                        UndoableAction(
                            message = Res.string.commitment_reopened,
                            argument = row.commitment.name,
                            commit = { Result.success(Unit) },
                            undo = { archive(row) },
                            restore = { load() },
                        ),
                    )
                }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
        }
    }

    private companion object {
        /** How far ahead the next due date of each commitment is looked for (yearly ones included). */
        const val NEXT_DUE_DAYS = 400
    }
}
