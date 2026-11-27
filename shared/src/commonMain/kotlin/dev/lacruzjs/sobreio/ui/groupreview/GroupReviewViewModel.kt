package dev.lacruzjs.sobreio.ui.groupreview

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.NEXT_DUE_WINDOW_DAYS
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.occurrence_recorded
import dev.lacruzjs.sobreio.generated.resources.occurrence_skipped
import dev.lacruzjs.sobreio.ui.format.formatMoneyInput
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import dev.lacruzjs.sobreio.ui.loading.revalidate
import dev.lacruzjs.sobreio.ui.validation.validatePositiveAmount
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

/**
 * The focused review of a group's due dates: the only place where they are confirmed or skipped, both the ones already due
 * and the next ones, which can be registered before their day (the Group tab only lists the next few and opens this screen). Every figure comes from the database; the app only joins names.
 */
class GroupReviewViewModel(
    private val groupId: String,
    private val groups: GroupRepository,
    private val summary: GroupSummaryRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val calendar: CalendarRepository,
    private val commitments: CommitmentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupReviewUiState())
    val state: StateFlow<GroupReviewUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var loadJob: Job? = null
    private var today: LocalDate? = null
    private var hasLoaded = false

    /** Skipped due dates whose skip is still waiting for the snackbar: a reload must not bring them back. */
    private val hiddenKeys = mutableSetOf<String>()

    init {
        // Painted from the cache before the first frame when it was seen recently
        load()
    }

    fun onEvent(event: GroupReviewEvent) {
        when (event) {
            GroupReviewEvent.Refresh -> {
                // A skip still waiting for its snackbar must be final before the due dates are read again
                undo.commit()
                load()
            }
            // Handled by the Route, which navigates; a skip still waiting is made final first, before the group reloads
            GroupReviewEvent.BackClicked -> undo.commitDetached()
            is GroupReviewEvent.ConfirmClicked -> openConfirm(event.key)
            is GroupReviewEvent.SkipClicked -> skip(event.key)
            is GroupReviewEvent.AmountChanged ->
                updateSheet { it.copy(amount = event.value, amountError = null, submitError = null) }
            is GroupReviewEvent.DateChanged -> updateSheet { it.copy(date = event.date, submitError = null) }
            is GroupReviewEvent.CoverSelected -> updateSheet { it.copy(coverEnvelopeId = event.envelopeId) }
            GroupReviewEvent.ConfirmSubmitted -> confirm()
            GroupReviewEvent.SheetDismissed -> _state.update { if (it.sheet?.isSubmitting == true) it else it.copy(sheet = null) }
            GroupReviewEvent.UndoClicked -> undo.undo()
            GroupReviewEvent.UndoExpired -> undo.commit()
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending skip still has to be made final
        undo.commitDetached()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            revalidate(onRefreshing = { refreshing -> _state.update { it.copy(isRefreshing = refreshing) } }) {
                val reads = coroutineScope {
                    val groupD = async { groups.getGroup(groupId) }
                    val todayD = async { calendar.getToday(groupId) }
                    val pendingD = async { summary.getPending(groupId) }
                    val upcomingD = async {
                        todayD.await().map { day -> day.plus(1, DateTimeUnit.DAY) to day.plus(NEXT_DUE_WINDOW_DAYS, DateTimeUnit.DAY) }
                            .mapCatching { (from, to) -> summary.getUpcoming(groupId, from, to).getOrThrow() }
                    }
                    val accountsD = async { accounts.getAccounts(groupId) }
                    val envelopesD = async { envelopes.getEnvelopes(groupId) }
                    Reads(groupD.await(), todayD.await(), pendingD.await(), upcomingD.await(), accountsD.await(), envelopesD.await())
                }
                val failure = reads.group.exceptionOrNull() ?: reads.today.exceptionOrNull() ?: reads.pending.exceptionOrNull()
                    ?: reads.upcoming.exceptionOrNull() ?: reads.accounts.exceptionOrNull() ?: reads.envelopes.exceptionOrNull()
                if (failure != null) {
                    _state.update {
                        if (!hasLoaded) it.copy(isLoading = false, error = failure.asAppError())
                        else it.copy(isLoading = false, actionError = failure.asAppError())
                    }
                    return@revalidate
                }
                val group = reads.group.getOrThrow()
                val day = reads.today.getOrThrow()
                today = day
                hasLoaded = true
                _state.update {
                    it.copy(
                        groupName = group.name,
                        currencyCode = group.currencyCode,
                        canRecord = !group.isArchived && !group.isPendingDeletion,
                        today = day,
                        pending = reads.pending.getOrThrow().filter { o -> o.isReviewable && o.key !in hiddenKeys },
                        upcoming = reads.upcoming.getOrThrow().filter { o -> o.isReviewable && o.key !in hiddenKeys }
                            .sortedWith(BY_DATE),
                        accounts = reads.accounts.getOrThrow(),
                        envelopes = reads.envelopes.getOrThrow(),
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    private fun openConfirm(key: String) {
        val current = _state.value
        if (!current.canRecord) return
        val occurrence = current.find(key) ?: return
        val limit = today ?: occurrence.dueDate
        _state.update {
            it.copy(
                sheet = ConfirmDraft(
                    occurrence = occurrence,
                    amount = formatMoneyInput(occurrence.amountCents),
                    date = if (occurrence.dueDate < limit) occurrence.dueDate else limit,
                ),
            )
        }
        loadReserve(occurrence)
    }

    /** Reads what is set aside for the payment; the sheet works without it, so a failure only hides the line. */
    private fun loadReserve(occurrence: DueOccurrence) {
        viewModelScope.launch {
            val funding = commitments.getFundingStatus(occurrence.commitmentId).getOrNull()
            val reserved = reservedForDue(occurrence, funding) ?: return@launch
            _state.update { s ->
                s.copy(sheet = s.sheet?.takeIf { it.occurrence.key == occurrence.key }?.copy(reservedCents = reserved) ?: s.sheet)
            }
        }
    }

    private fun confirm() {
        val draft = _state.value.sheet ?: return
        if (draft.isSubmitting) return
        val cents = parseMoneyInput(draft.amount)
        val amountError = validatePositiveAmount(cents)
        if (amountError != null || cents == null) {
            updateSheet { it.copy(amountError = amountError) }
            return
        }
        val limit = today
        if (limit != null && draft.date > limit) {
            updateSheet { it.copy(submitError = AppError.BusinessRule("FZ003")) }
            return
        }
        val current = _state.value
        val cover = if (current.coverShortEnvelope(draft) != null) {
            current.envelopes.firstOrNull { it.id == draft.coverEnvelopeId }?.let { listOf(CoverItem(it.id, it.balanceCents)) }.orEmpty()
        } else {
            emptyList()
        }
        val occurrence = draft.occurrence
        updateSheet { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            summary.confirm(occurrence.commitmentId, occurrence.kind, occurrence.dueDate, cents, draft.date, cover)
                .onSuccess { fact ->
                    _state.update { it.without(occurrence.key).copy(sheet = null, actionError = null) }
                    undo.show(
                        UndoableAction(
                            message = Res.string.occurrence_recorded,
                            argument = occurrence.name,
                            commit = { Result.success(Unit) },
                            undo = { summary.voidConfirmed(fact) },
                            restore = { load() },
                        ),
                    )
                    // The envelope balances changed, which the cover note reads
                    load()
                }
                .onFailure { e -> updateSheet { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** No confirmation: the row goes at once and the skip is written when the snackbar ends without "Undo". */
    private fun skip(key: String) {
        val current = _state.value
        if (!current.canRecord) return
        val occurrence = current.find(key) ?: return
        val wasPending = current.pending.any { it.key == key }
        hiddenKeys += key
        _state.update { it.without(key).copy(actionError = null) }
        val restore = {
            hiddenKeys -= key
            _state.update { s ->
                if (wasPending) s.copy(pending = (s.pending + occurrence).sortedWith(BY_DATE))
                else s.copy(upcoming = (s.upcoming + occurrence).sortedWith(BY_DATE))
            }
        }
        undo.show(
            UndoableAction(
                message = Res.string.occurrence_skipped,
                argument = occurrence.name,
                commit = {
                    summary.skip(occurrence.commitmentId, occurrence.dueDate).onSuccess {
                        hiddenKeys -= key
                        load()
                    }
                },
                undo = { Result.success(Unit) },
                restore = restore,
            ),
        )
    }

    private fun GroupReviewUiState.find(key: String): DueOccurrence? =
        pending.firstOrNull { it.key == key } ?: upcoming.firstOrNull { it.key == key }

    private fun GroupReviewUiState.without(key: String): GroupReviewUiState =
        copy(pending = pending.filterNot { it.key == key }, upcoming = upcoming.filterNot { it.key == key })

    private fun updateSheet(change: (ConfirmDraft) -> ConfirmDraft) =
        _state.update { s -> s.copy(sheet = s.sheet?.let(change)) }


    private class Reads(
        val group: Result<dev.lacruzjs.sobreio.domain.model.Group>,
        val today: Result<LocalDate>,
        val pending: Result<List<DueOccurrence>>,
        val upcoming: Result<List<DueOccurrence>>,
        val accounts: Result<List<dev.lacruzjs.sobreio.domain.model.Account>>,
        val envelopes: Result<List<dev.lacruzjs.sobreio.domain.model.Envelope>>,
    )

    private companion object {
        val BY_DATE = compareBy<DueOccurrence>({ it.dueDate }, { it.name })
    }
}
