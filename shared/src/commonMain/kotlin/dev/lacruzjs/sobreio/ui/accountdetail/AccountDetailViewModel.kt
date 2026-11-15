package dev.lacruzjs.sobreio.ui.accountdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.domain.model.shortfallDate
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.adjustment_recorded
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * One account's detail: balance, charts, forecast, due items, latest movements and the comparison with the bank. Every
 * figure comes from the database through [AccountDetailRepository]; this class only chooses what to ask and joins names.
 * [debounceMillis] is how long typing in the comparison sheet must pause before the database is asked.
 */
class AccountDetailViewModel(
    private val groupId: String,
    private val accountId: String,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val members: MemberRepository,
    private val envelopes: EnvelopeRepository,
    private val detail: AccountDetailRepository,
    private val debounceMillis: Long = COMPARE_DEBOUNCE_MILLIS,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDetailUiState())
    val state: StateFlow<AccountDetailUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var loadJob: Job? = null
    private var seriesJob: Job? = null
    private var compareJob: Job? = null
    private var hasLoaded = false

    fun onEvent(event: AccountDetailEvent) {
        when (event) {
            AccountDetailEvent.Refresh -> {
                // An adjustment still waiting for its snackbar is final before the screen is read again
                undo.commit()
                load(showSpinner = !hasLoaded)
            }
            AccountDetailEvent.Retry -> load(showSpinner = true)
            is AccountDetailEvent.RangeSelected -> selectRange(event.range)
            AccountDetailEvent.CompareClicked -> _state.update {
                // A debt on the account suggests a debt on the bank's side too
                it.copy(compare = CompareState(bankIsNegative = (it.account?.balanceCents ?: 0L) < 0L))
            }
            AccountDetailEvent.CompareDismissed -> {
                compareJob?.cancel()
                _state.update { it.copy(compare = null) }
            }
            is AccountDetailEvent.BankBalanceChanged -> bankBalanceChanged(event.value, _state.value.compare?.bankIsNegative ?: false)
            is AccountDetailEvent.BankSignChanged -> {
                val compare = _state.value.compare ?: return
                bankBalanceChanged(compare.bankInput, event.negative)
            }
            AccountDetailEvent.AdjustClicked -> adjust()
            AccountDetailEvent.UndoClicked -> undo.undo()
            AccountDetailEvent.UndoExpired -> undo.commit()
            // Handled by the Route
            AccountDetailEvent.BackClicked,
            AccountDetailEvent.EditClicked,
            AccountDetailEvent.SeeAllClicked,
            is AccountDetailEvent.TabSelected,
            -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    private fun load(showSpinner: Boolean) {
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        loadJob?.cancel()
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
        val accountsResult = accounts.getAccounts(groupId)
        (groupResult.exceptionOrNull() ?: accountsResult.exceptionOrNull())?.let { return it.asAppError() }
        val group = groupResult.getOrThrow()
        val allAccounts = accountsResult.getOrThrow()
        val account = allAccounts.firstOrNull { it.id == accountId } ?: return AppError.NotFound
        val today = detail.getToday(groupId).getOrElse { return it.asAppError() }
        val range = _state.value.range
        val from = today.minus(range.months, DateTimeUnit.MONTH)
        val upcomingTo = today.plus(UPCOMING_MONTHS, DateTimeUnit.MONTH)

        return coroutineScope {
            val period = async { detail.getCurrentPeriod(groupId) }
            val forecast = async { detail.getForecast(groupId, accountId) }
            val series = async { detail.getBalanceSeries(accountId, from, today) }
            val projection = async { detail.getProjection(accountId) }
            val flows = async { detail.getPeriodFlows(accountId, FLOW_PERIODS) }
            val upcoming = async { detail.getUpcoming(groupId, accountId, today, upcomingTo) }
            val movements = async { detail.getLatestMovements(accountId, LATEST_MOVEMENTS) }
            val memberList = async { members.getMembers(groupId) }
            val envelopeList = async { envelopes.getEnvelopes(groupId) }

            val periodResult = period.await()
            val forecastResult = forecast.await()
            val seriesResult = series.await()
            val projectionResult = projection.await()
            val flowsResult = flows.await()
            val upcomingResult = upcoming.await()
            val movementsResult = movements.await()
            val failure = periodResult.exceptionOrNull()
                ?: forecastResult.exceptionOrNull()
                ?: seriesResult.exceptionOrNull()
                ?: projectionResult.exceptionOrNull()
                ?: flowsResult.exceptionOrNull()
                ?: upcomingResult.exceptionOrNull()
                ?: movementsResult.exceptionOrNull()
            if (failure != null) return@coroutineScope failure.asAppError()

            // Only names to show: without them the rows simply lack an author or an envelope
            val people = memberList.await().getOrDefault(emptyList())
            val envelopeNames = envelopeList.await().getOrDefault(emptyList())
            val accountForecast = forecastResult.getOrThrow()
            val projectionRows = projectionResult.getOrThrow()
            val shortfall = accountForecast?.takeIf { it.shortfallCents > 0 }
                ?.let { shortfallDate(projectionRows, it.projectedMinCents) }

            _state.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    actionError = null,
                    groupName = group.name,
                    currencyCode = group.currencyCode,
                    canEdit = group.canEdit,
                    account = account,
                    holderName = people.firstOrNull { m -> m.userId == account.ownerUserId }?.displayName,
                    hasSeveralMembers = people.hasSeveralMembers(),
                    today = today,
                    periodEnd = periodResult.getOrThrow().end,
                    balanceSeries = seriesResult.getOrThrow(),
                    isSeriesLoading = false,
                    projection = projectionRows,
                    forecast = accountForecast,
                    shortfallDate = shortfall,
                    flows = flowsResult.getOrThrow(),
                    upcoming = upcomingResult.getOrThrow().take(UPCOMING_ITEMS),
                    movements = movementsResult.getOrThrow().map { m -> m.toRow(people, envelopeNames, allAccounts.associate { a -> a.id to a.name }) },
                )
            }
            hasLoaded = true
            null
        }
    }

    private fun AccountMovement.toRow(
        people: List<Member>,
        envelopeList: List<Envelope>,
        accountNames: Map<String, String>,
    ): MovementRowState {
        val envelope = envelopeId?.let { id -> envelopeList.firstOrNull { it.id == id } }
        return MovementRowState(
            movement = this,
            authorName = people.firstOrNull { it.userId == createdBy }?.displayName,
            envelopeName = envelope?.takeIf { it.kind != EnvelopeKind.System }?.name,
            envelopeIsUnassigned = envelope?.kind == EnvelopeKind.System,
            counterAccountName = counterAccountId?.let { accountNames[it] },
        )
    }

    private fun selectRange(range: BalanceRange) {
        val current = _state.value
        val today = current.today ?: return
        if (range == current.range) return
        _state.update { it.copy(range = range, isSeriesLoading = true, actionError = null) }
        seriesJob?.cancel()
        seriesJob = viewModelScope.launch {
            detail.getBalanceSeries(accountId, today.minus(range.months, DateTimeUnit.MONTH), today)
                .onSuccess { points -> _state.update { it.copy(balanceSeries = points, isSeriesLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isSeriesLoading = false, actionError = e.asAppError()) } }
        }
    }

    private fun bankBalanceChanged(text: String, negative: Boolean) {
        val compare = _state.value.compare?.copy(bankIsNegative = negative) ?: return
        compareJob?.cancel()
        val cents = parseMoneyInput(text)?.let { if (negative) -abs(it) else abs(it) }
        when {
            // Nothing to compare yet
            text.isBlank() ->
                setCompare(compare.copy(bankInput = text, result = null, isComparing = false, inputError = null, error = null))
            cents == null ->
                setCompare(compare.copy(bankInput = text, result = null, isComparing = false, inputError = Res.string.validation_amount_invalid, error = null))
            else -> {
                setCompare(compare.copy(bankInput = text, result = null, isComparing = true, inputError = null, error = null))
                compareJob = viewModelScope.launch {
                    // Typing pauses before the database is asked: only the last value is compared
                    delay(debounceMillis)
                    detail.compareWithBank(accountId, cents)
                        .onSuccess { result ->
                            updateCompare { it.copy(result = result, isComparing = false) }
                        }
                        .onFailure { e ->
                            updateCompare { it.copy(isComparing = false, error = e.asAppError()) }
                        }
                }
            }
        }
    }

    private fun adjust() {
        val compare = _state.value.compare ?: return
        val difference = compare.result?.differenceCents ?: return
        if (difference == 0L || compare.isAdjusting) return
        updateCompare { it.copy(isAdjusting = true, error = null) }
        viewModelScope.launch {
            detail.recordAdjustment(accountId, difference)
                .onSuccess { transactionId ->
                    _state.update { it.copy(compare = null) }
                    undo.show(
                        UndoableAction(
                            message = Res.string.adjustment_recorded,
                            argument = "",
                            amountCents = difference,
                            // Nothing waits to be done: the adjustment is already in the ledger
                            commit = { Result.success(Unit) },
                            // The ledger is never deleted: undoing voids the transaction
                            undo = { detail.voidTransaction(transactionId) },
                            restore = { load(showSpinner = false) },
                        ),
                    )
                    load(showSpinner = false)
                }
                .onFailure { e -> updateCompare { it.copy(isAdjusting = false, error = e.asAppError()) } }
        }
    }

    private fun setCompare(value: CompareState) = _state.update { it.copy(compare = value) }

    private fun updateCompare(change: (CompareState) -> CompareState) =
        _state.update { state -> state.copy(compare = state.compare?.let(change)) }

    companion object {
        const val COMPARE_DEBOUNCE_MILLIS = 400L
        const val FLOW_PERIODS = 6
        const val LATEST_MOVEMENTS = 5
        const val UPCOMING_ITEMS = 3
        private const val UPCOMING_MONTHS = 3
    }
}
