package dev.lacruzjs.sobreio.ui.groupsummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.domain.model.shortfallDate
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.occurrence_recorded
import dev.lacruzjs.sobreio.generated.resources.occurrence_skipped
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
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
 * The Group tab: the money really free, the due dates to review (the only place where they are confirmed or skipped),
 * the warnings and the next due dates. Every figure comes from the database; the app only joins names and signs due items.
 */
class GroupSummaryViewModel(
    private val groupId: String,
    private val summary: GroupSummaryRepository,
    private val detail: AccountDetailRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupSummaryUiState())
    val state: StateFlow<GroupSummaryUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var loadJob: Job? = null
    private var today: LocalDate? = null

    /** Skipped due dates whose skip is still waiting for the snackbar: a reload must not bring them back. */
    private val hiddenKeys = mutableSetOf<String>()

    fun onEvent(event: GroupSummaryEvent) {
        when (event) {
            is GroupSummaryEvent.GroupLoaded ->
                _state.update { it.copy(currencyCode = event.currencyCode, canRecord = event.canRecord) }
            GroupSummaryEvent.Refresh -> {
                // A skip still waiting for its snackbar must be final before the due dates are read again
                undo.commit()
                load()
            }
            is GroupSummaryEvent.ConfirmClicked -> openConfirm(event.key)
            is GroupSummaryEvent.SkipClicked -> skip(event.key)
            is GroupSummaryEvent.AmountChanged ->
                updateSheet { it.copy(amount = event.value, amountError = null, submitError = null) }
            is GroupSummaryEvent.DateChanged -> updateSheet { it.copy(date = event.date, submitError = null) }
            is GroupSummaryEvent.CoverSelected -> updateSheet { it.copy(coverEnvelopeId = event.envelopeId) }
            GroupSummaryEvent.ConfirmSubmitted -> confirm()
            GroupSummaryEvent.SheetDismissed -> _state.update { if (it.sheet?.isSubmitting == true) it else it.copy(sheet = null) }
            // Handled by the Route, which navigates
            is GroupSummaryEvent.WarningClicked -> Unit
            GroupSummaryEvent.UndoClicked -> undo.undo()
            GroupSummaryEvent.UndoExpired -> undo.commit()
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending skip still has to be made final
        undo.commitDetached()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val reads = coroutineScope {
                val todayD = async { detail.getToday(groupId) }
                val outlookD = async { summary.getOutlook(groupId) }
                val pendingD = async { summary.getPending(groupId) }
                val accountsD = async { accounts.getAccounts(groupId) }
                val envelopesD = async { envelopes.getEnvelopes(groupId) }
                val progressD = async { envelopes.getProgress(groupId) }
                val shortfallsD = async { summary.getShortfalls(groupId) }
                Reads(
                    todayD.await(), outlookD.await(), pendingD.await(), accountsD.await(),
                    envelopesD.await(), progressD.await(), shortfallsD.await(),
                )
            }
            val failure = reads.today.exceptionOrNull() ?: reads.outlook.exceptionOrNull() ?: reads.pending.exceptionOrNull()
                ?: reads.accounts.exceptionOrNull() ?: reads.envelopes.exceptionOrNull()
            if (failure != null) {
                _state.update {
                    if (it.outlook == null) it.copy(isLoading = false, error = failure.asAppError())
                    else it.copy(isLoading = false, actionError = failure.asAppError())
                }
                return@launch
            }
            val day = reads.today.getOrThrow()
            val outlook = reads.outlook.getOrThrow()
            val accountList = reads.accounts.getOrThrow()
            val envelopeList = reads.envelopes.getOrThrow()
            today = day
            // The warnings and the next due dates are secondary: a failure leaves them out instead of the whole tab
            val upcomingFrom = day.plus(1, DateTimeUnit.DAY)
            val upcoming = if (upcomingFrom > outlook.periodEnd) {
                emptyList()
            } else {
                summary.getUpcoming(groupId, upcomingFrom, outlook.periodEnd).getOrDefault(emptyList())
            }
            val warnings = buildList {
                reads.progress.getOrDefault(emptyList())
                    .filter { it.state == ProgressState.Overspent }
                    .mapNotNull { p -> envelopeList.firstOrNull { it.id == p.envelopeId }?.let { GroupWarning.EnvelopeOverspent(it.id, it.name, abs(p.currentCents)) } }
                    .sortedBy { it.name.lowercase() }
                    .let { addAll(it) }
                reads.shortfalls.getOrDefault(emptyList()).forEach { shortfall ->
                    val account = accountList.firstOrNull { it.id == shortfall.accountId } ?: return@forEach
                    val date = detail.getProjection(account.id).getOrNull()?.let { shortfallDate(it, shortfall.lowestCents) }
                    add(GroupWarning.AccountShortfallOn(account.id, account.name, shortfall.lowestCents, date ?: outlook.periodEnd))
                }
                if (outlook.committedShortfallCents > 0) add(GroupWarning.CommitmentsShortfall(outlook.committedShortfallCents))
            }
            _state.update {
                it.copy(
                    outlook = outlook,
                    today = day,
                    pending = reads.pending.getOrThrow().filter { o -> o.isReviewable && o.key !in hiddenKeys },
                    upcoming = upcoming,
                    warnings = warnings,
                    accounts = accountList,
                    envelopes = envelopeList,
                    isLoading = false,
                    error = null,
                )
            }
        }
    }

    private fun openConfirm(key: String) {
        val current = _state.value
        if (!current.canRecord) return
        val occurrence = current.pending.firstOrNull { it.key == key } ?: return
        val limit = today ?: occurrence.dueDate
        _state.update {
            it.copy(
                sheet = ConfirmDraft(
                    occurrence = occurrence,
                    amount = occurrence.amountCents.toInputText(),
                    date = if (occurrence.dueDate < limit) occurrence.dueDate else limit,
                ),
            )
        }
    }

    private fun confirm() {
        val draft = _state.value.sheet ?: return
        if (draft.isSubmitting) return
        val cents = parseMoneyInput(draft.amount)
        val amountError = when {
            cents == null -> Res.string.validation_amount_invalid
            cents <= 0 -> Res.string.validation_amount_positive
            else -> null
        }
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
                    _state.update { it.copy(sheet = null, pending = it.pending.filterNot { o -> o.key == occurrence.key }, actionError = null) }
                    undo.show(
                        UndoableAction(
                            message = Res.string.occurrence_recorded,
                            argument = occurrence.name,
                            commit = { Result.success(Unit) },
                            undo = { summary.voidConfirmed(fact) },
                            restore = { load() },
                        ),
                    )
                    // The figures changed: the free money, the warnings and the next due dates
                    load()
                }
                .onFailure { e -> updateSheet { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** No confirmation: the row goes at once and the skip is written when the snackbar ends without "Undo". */
    private fun skip(key: String) {
        val current = _state.value
        if (!current.canRecord) return
        val occurrence = current.pending.firstOrNull { it.key == key } ?: return
        hiddenKeys += key
        _state.update { it.copy(pending = it.pending - occurrence, actionError = null) }
        val restore = {
            hiddenKeys -= key
            _state.update { s -> s.copy(pending = (s.pending + occurrence).sortedWith(compareBy({ it.dueDate }, { it.name }))) }
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

    private fun updateSheet(change: (ConfirmDraft) -> ConfirmDraft) =
        _state.update { s -> s.copy(sheet = s.sheet?.let(change)) }

    /** Cents as the text the user would type: a plain point. */
    private fun Long.toInputText(): String = when {
        this <= 0 -> ""
        this % 100 == 0L -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }

    private class Reads(
        val today: Result<LocalDate>,
        val outlook: Result<dev.lacruzjs.sobreio.domain.model.GroupOutlook>,
        val pending: Result<List<DueOccurrence>>,
        val accounts: Result<List<dev.lacruzjs.sobreio.domain.model.Account>>,
        val envelopes: Result<List<dev.lacruzjs.sobreio.domain.model.Envelope>>,
        val progress: Result<List<dev.lacruzjs.sobreio.domain.model.EnvelopeProgress>>,
        val shortfalls: Result<List<dev.lacruzjs.sobreio.domain.model.AccountShortfall>>,
    )
}
