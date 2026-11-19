package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.data.repository.MovementRepository
import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.contribution_deleted
import dev.lacruzjs.sobreio.generated.resources.movement_deleted
import dev.lacruzjs.sobreio.ui.undo.PendingUndoCommits
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import dev.lacruzjs.sobreio.ui.loading.revalidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The group's ledger: the Movements tab ([fixedAccountId] and [fixedEnvelopeId] null) or the level 3 list of one account or
 * one envelope. Filters and the
 * search text live here, so they last while the screen does and are not saved on disk. The database filters, orders and
 * pages (`search_movements`); this class chooses what to ask, joins names and hides a row while its delete waits for the
 * snackbar. [searchDebounceMillis] is how long typing must pause before the database is asked.
 */
class MovementsViewModel(
    private val groupId: String,
    private val fixedAccountId: String?,
    private val fixedEnvelopeId: String?,
    private val movements: MovementRepository,
    private val groups: GroupRepository,
    private val accounts: AccountRepository,
    private val envelopes: EnvelopeRepository,
    private val members: MemberRepository,
    private val tags: TagRepository,
    /** Tags the list starts filtered by (a bank import's tag); the Tags chip can clear them. */
    initialTagIds: Set<String> = emptySet(),
    private val searchDebounceMillis: Long = SEARCH_DEBOUNCE_MILLIS,
    private val calendar: CalendarRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        MovementsUiState(
            fixedAccountId = fixedAccountId,
            fixedEnvelopeId = fixedEnvelopeId,
            filter = MovementFilter(accountId = fixedAccountId, envelopeId = fixedEnvelopeId, tagIds = initialTagIds),
        ),
    )
    val state: StateFlow<MovementsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    private var searchJob: Job? = null

    /** Bumped by every reload, so a slow answer to an old filter is dropped. */
    private var generation = 0

    /** Rows of the database already read (hidden ones included) and how many match the filter in all. */
    private var fetched = 0
    private var serverTotal = 0L

    fun onEvent(event: MovementsEvent) {
        when (event) {
            MovementsEvent.Refresh -> {
                // A delete still waiting for its snackbar must be final before the list is read again
                undo.commit()
                loadAll()
            }
            MovementsEvent.LoadMore, MovementsEvent.RetryLoadMore -> loadMore()
            MovementsEvent.SearchOpened -> _state.update { it.copy(searchOpen = true) }
            MovementsEvent.SearchClosed -> closeSearch()
            is MovementsEvent.SearchChanged -> changeSearch(event.text)
            is MovementsEvent.ChipClicked -> _state.update { it.copy(sheet = event.chip.sheet()) }
            is MovementsEvent.ChipCleared -> clearChip(event.chip)
            MovementsEvent.ClearAllFilters -> applyFilter(
                MovementFilter(accountId = fixedAccountId, envelopeId = fixedEnvelopeId, query = _state.value.filter.query),
                datePreset = null,
            )
            MovementsEvent.SheetDismissed -> _state.update { it.copy(sheet = MovementsSheet.None) }
            is MovementsEvent.AccountSelected -> applyFilter(_state.value.filter.copy(accountId = event.accountId))
            is MovementsEvent.EnvelopeSelected -> applyFilter(_state.value.filter.copy(envelopeId = event.envelopeId))
            is MovementsEvent.TypeSelected -> applyFilter(_state.value.filter.copy(type = event.type))
            is MovementsEvent.DatePresetSelected -> selectPreset(event.preset)
            is MovementsEvent.CustomRangeSelected ->
                applyFilter(_state.value.filter.copy(from = event.from, to = event.to), DatePreset.Custom)
            MovementsEvent.CustomRangeDismissed -> _state.update { it.copy(customRangeOpen = false) }
            is MovementsEvent.TagsApplied -> applyFilter(_state.value.filter.copy(tagIds = event.tagIds))
            is MovementsEvent.PersonSelected -> applyFilter(_state.value.filter.copy(createdBy = event.userId))
            MovementsEvent.NewClicked, is MovementsEvent.EditClicked -> Unit
            MovementsEvent.ReportOpened -> loadReport()
            is MovementsEvent.DeleteClicked -> delete(event.movementId)
            MovementsEvent.UndoClicked -> undo.undo()
            MovementsEvent.UndoExpired -> undo.commit()
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    private fun MovementsChip.sheet() = when (this) {
        MovementsChip.Account -> MovementsSheet.Account
        MovementsChip.Envelope -> MovementsSheet.Envelope
        MovementsChip.Type -> MovementsSheet.Type
        MovementsChip.Dates -> MovementsSheet.Dates
        MovementsChip.Tags -> MovementsSheet.Tags
        MovementsChip.Person -> MovementsSheet.Person
    }

    // Loading ------------------------------------------------------------------------------------------------------

    init {
        // Painted from the cache before the first frame when the ledger was seen recently; read when it was not
        loadAll()
    }

    private var loadJob: Job? = null

    private fun setRefreshing(refreshing: Boolean) = _state.update { it.copy(isRefreshing = refreshing) }

    /** The group, the names the rows and chips need, the date presets and the first page. */
    private fun loadAll() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // A delete made final as the previous screen closed is written before reading
            PendingUndoCommits.await()
            revalidate(::setRefreshing) { readAll() }
        }
    }

    private suspend fun readAll() {
        groups.getGroup(groupId)
            .onSuccess { group ->
                _state.update {
                    it.copy(groupName = group.name, currencyCode = group.currencyCode, canEdit = group.canEdit)
                }
            }
            .onFailure { e ->
                _state.update {
                    if (it.isLoading) it.copy(isLoading = false, error = e.asAppError()) else it.copy(actionError = e.asAppError())
                }
                return
            }
        // Names and options only decorate the list: without them rows show less, they do not fail
        accounts.getAccounts(groupId).onSuccess { list -> _state.update { it.copy(accounts = list) } }
        envelopes.getEnvelopes(groupId).onSuccess { list -> _state.update { it.copy(envelopes = list) } }
        members.getMembers(groupId).onSuccess { list -> _state.update { it.copy(members = list) } }
        tags.getTags(groupId).onSuccess { list -> _state.update { it.copy(tags = list) } }
        calendar.getDateContext(groupId).onSuccess { context -> _state.update { it.copy(dateContext = context) } }
        readFirstPage(++generation)
    }

    /**
     * Reads the first page again for the current filter. The rows on screen stay, with the thin bar, until the answer
     * arrives; a filter seen in the last 30 s answers at once from the cache.
     */
    private fun reloadPage() {
        val request = ++generation
        _state.update { it.copy(error = null, isLoadingMore = false, loadMoreFailed = false, isRefreshing = true) }
        viewModelScope.launch {
            revalidate(onRefreshing = {}) { readFirstPage(request) }
            if (request == generation) setRefreshing(false)
        }
    }

    private suspend fun readFirstPage(request: Int) {
        val filter = _state.value.filter
        movements.search(groupId, filter, PAGE_SIZE, 0, withTotal = true)
            .onSuccess { page ->
                if (request != generation) return@onSuccess
                fetched = page.items.size
                serverTotal = page.total
                _state.update {
                    it.copy(items = page.items, hasMore = fetched < serverTotal, isLoading = false, error = null)
                }
            }
            .onFailure { e ->
                if (request != generation) return@onFailure
                _state.update { it.copy(isLoading = false, error = e.asAppError()) }
            }
        // The totals of this filter, after the rows so they never hold the list back; secondary: without them the card
        // is simply not shown
        movements.getTotals(groupId, filter).let { result ->
            if (request == generation) _state.update { it.copy(totals = result.getOrNull(), report = null) }
        }
    }

    /** The report page of the current filter; a failure leaves the page with the totals only. */
    private fun loadReport() {
        val filter = _state.value.filter
        viewModelScope.launch {
            movements.getReport(groupId, filter).onSuccess { report ->
                if (_state.value.filter == filter) _state.update { it.copy(report = report) }
            }
        }
    }

    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        val request = generation
        _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
        viewModelScope.launch {
            movements.search(groupId, current.filter, PAGE_SIZE, fetched, withTotal = false)
                .onSuccess { page ->
                    if (request != generation) return@onSuccess
                    fetched += page.items.size
                    _state.update {
                        // An empty page ends the list even if the total said otherwise (rows voided meanwhile)
                        val more = page.items.isNotEmpty() && fetched < serverTotal
                        it.copy(items = it.items + page.items, hasMore = more, isLoadingMore = false)
                    }
                }
                .onFailure {
                    if (request != generation) return@onFailure
                    _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) }
                }
        }
    }

    // Filters ------------------------------------------------------------------------------------------------------

    private fun applyFilter(filter: MovementFilter, datePreset: DatePreset? = _state.value.datePreset) {
        // A hidden row would come back with the new page: its delete must be final first
        undo.commit()
        _state.update {
            it.copy(filter = filter, datePreset = datePreset, sheet = MovementsSheet.None, customRangeOpen = false)
        }
        reloadPage()
    }

    private fun clearChip(chip: MovementsChip) {
        val filter = _state.value.filter
        when (chip) {
            MovementsChip.Account -> applyFilter(filter.copy(accountId = fixedAccountId))
            MovementsChip.Envelope -> applyFilter(filter.copy(envelopeId = fixedEnvelopeId))
            MovementsChip.Type -> applyFilter(filter.copy(type = null))
            MovementsChip.Dates -> applyFilter(filter.copy(from = null, to = null), datePreset = null)
            MovementsChip.Tags -> applyFilter(filter.copy(tagIds = emptySet()))
            MovementsChip.Person -> applyFilter(filter.copy(createdBy = null))
        }
    }

    private fun selectPreset(preset: DatePreset?) {
        val filter = _state.value.filter
        when (preset) {
            null -> applyFilter(filter.copy(from = null, to = null), datePreset = null)
            DatePreset.Custom -> _state.update { it.copy(sheet = MovementsSheet.None, customRangeOpen = true) }
            else -> {
                val range = _state.value.dateContext?.let { datePresetRange(preset, it) } ?: return
                applyFilter(filter.copy(from = range.first, to = range.second), preset)
            }
        }
    }

    // Search -------------------------------------------------------------------------------------------------------

    private fun changeSearch(text: String) {
        _state.update { it.copy(searchText = text) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(searchDebounceMillis)
            applyQuery(text)
        }
    }

    private fun closeSearch() {
        searchJob?.cancel()
        _state.update { it.copy(searchOpen = false, searchText = "") }
        applyQuery("")
    }

    private fun applyQuery(text: String) {
        val query = text.trim()
        val filter = _state.value.filter
        if (query == filter.query) return
        applyFilter(filter.copy(query = query))
    }

    // Delete -------------------------------------------------------------------------------------------------------

    /**
     * No confirmation: the row disappears at once and "Undo" follows. The void is written only when the snackbar ends
     * without Undo (or when something else needs it final), like leaving a group. A contribution leg voids the whole
     * contribution (both legs, in both groups) with `void_group_transfer`; the other leg lives in the other group's ledger.
     */
    private fun delete(movementId: String) {
        val movement = _state.value.items.firstOrNull { it.id == movementId && it.canDelete } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update { it.copy(items = it.items - movement, actionError = null) }
        undo.show(
            UndoableAction(
                message = if (movement.isGroupTransfer) Res.string.contribution_deleted else Res.string.movement_deleted,
                argument = "",
                amountCents = abs(movement.amountCents),
                commit = {
                    val void = if (movement.isGroupTransfer) {
                        movements.voidGroupTransfer(movement)
                    } else {
                        movements.voidMovement(movement)
                    }
                    void.onSuccess {
                        // The database no longer has the row, so the next page starts one row earlier
                        fetched = (fetched - 1).coerceAtLeast(0)
                        serverTotal = (serverTotal - 1).coerceAtLeast(0)
                    }
                },
                undo = { Result.success(Unit) },
                restore = { _state.update { it.copy(items = it.items.withBack(movement)) } },
            ),
        )
    }

    /** Puts [movement] back where the database's order (date, then entry time, then id) places it. */
    private fun List<Movement>.withBack(movement: Movement): List<Movement> =
        (this + movement).sortedWith(
            compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt }.thenBy { it.id },
        )

    companion object {
        const val PAGE_SIZE = 50
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
