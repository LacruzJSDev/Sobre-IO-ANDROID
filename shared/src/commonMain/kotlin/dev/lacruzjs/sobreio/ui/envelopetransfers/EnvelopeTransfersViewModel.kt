package dev.lacruzjs.sobreio.ui.envelopetransfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.transfer_deleted
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A move between envelopes of the group with both envelopes resolved ([from] and [to] null when not known, e.g. the
 * hidden "Outside envelopes"). [date] is the day it was entered in the group's time zone.
 */
data class GroupTransferRow(
    val transfer: EnvelopeTransfer,
    val from: Envelope?,
    val to: Envelope?,
    val date: LocalDate,
) {
    val canDelete: Boolean get() = transfer.origin.canVoid
    val canEdit: Boolean get() = transfer.origin.canEdit
}

data class EnvelopeTransfersUiState(
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val groupName: String = "",
    val currencyCode: String = "EUR",
    /** Any member of a live group may create, edit and void moves. */
    val canWrite: Boolean = false,
    /** The live moves loaded so far, newest first, minus the one hidden while its delete waits for the snackbar. */
    val rows: List<GroupTransferRow> = emptyList(),
    /** How many live moves the group has in all (the hidden one does not count). */
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** Failure of a delete, an undo or a refresh, shown above the list. */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
)

sealed interface EnvelopeTransfersEvent {
    /** Sent each time the screen is shown (also when coming back from the form). */
    data object Refresh : EnvelopeTransfersEvent
    data object Retry : EnvelopeTransfersEvent
    /** The list scrolled near its end, or its "Retry" was tapped. */
    data object LoadMore : EnvelopeTransfersEvent
    /** Swipe Delete: the move disappears at once and an Undo snackbar follows. */
    data class DeleteClicked(val transferId: String) : EnvelopeTransfersEvent
    data object UndoClicked : EnvelopeTransfersEvent
    data object UndoExpired : EnvelopeTransfersEvent

    /** Navigation, handled by the Route: the ViewModel ignores them. */
    data object BackClicked : EnvelopeTransfersEvent
    data object NewClicked : EnvelopeTransfersEvent
    data class EditClicked(val transferId: String) : EnvelopeTransfersEvent
}

/**
 * Every live move between envelopes of a group, newest first, paged. Edit opens the form; Delete voids the move after the
 * Undo snackbar. A voided monthly target is not generated again in its period (D-53).
 */
class EnvelopeTransfersViewModel(
    private val groupId: String,
    private val groups: GroupRepository,
    private val envelopes: EnvelopeRepository,
    private val transfers: EnvelopeTransferRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeTransfersUiState())
    val state: StateFlow<EnvelopeTransfersUiState> = _state.asStateFlow()

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
    private var envelopeById: Map<String, Envelope> = emptyMap()

    fun onEvent(event: EnvelopeTransfersEvent) {
        when (event) {
            EnvelopeTransfersEvent.Refresh -> {
                // A delete still waiting for its snackbar is final before the list is read again
                undo.commit()
                load(showSpinner = !hasLoaded)
            }
            EnvelopeTransfersEvent.Retry -> load(showSpinner = true)
            EnvelopeTransfersEvent.LoadMore -> loadMore()
            is EnvelopeTransfersEvent.DeleteClicked -> delete(event.transferId)
            EnvelopeTransfersEvent.UndoClicked -> undo.undo()
            EnvelopeTransfersEvent.UndoExpired -> undo.commit()
            EnvelopeTransfersEvent.BackClicked,
            EnvelopeTransfersEvent.NewClicked,
            is EnvelopeTransfersEvent.EditClicked,
            -> Unit
        }
    }

    override fun onCleared() {
        undo.commitDetached()
    }

    private fun load(showSpinner: Boolean) {
        if (showSpinner) _state.update { it.copy(isLoading = true, error = null) }
        loadJob?.cancel()
        loadMoreJob?.cancel()
        loadJob = viewModelScope.launch {
            val failure = loadAll()
            if (failure != null) {
                if (!hasLoaded) _state.update { it.copy(isLoading = false, error = failure) }
                else _state.update { it.copy(actionError = failure) }
            }
        }
    }

    private suspend fun loadAll(): AppError? = coroutineScope {
        val group = async { groups.getGroup(groupId) }
        val envelopeList = async { envelopes.getEnvelopes(groupId) }
        val page = async { transfers.getTransfers(groupId, 0, PAGE_SIZE) }
        val groupResult = group.await()
        val envelopeResult = envelopeList.await()
        val pageResult = page.await()
        (groupResult.exceptionOrNull() ?: envelopeResult.exceptionOrNull() ?: pageResult.exceptionOrNull())
            ?.let { return@coroutineScope it.asAppError() }
        val g = groupResult.getOrThrow()
        zone = runCatching { TimeZone.of(g.timezone) }.getOrDefault(TimeZone.UTC)
        envelopeById = envelopeResult.getOrThrow().associateBy { it.id }
        val items = pageResult.getOrThrow()
        fetched = items.items.size
        serverTotal = items.total
        _state.update {
            it.copy(
                isLoading = false,
                error = null,
                actionError = null,
                groupName = g.name,
                currencyCode = g.currencyCode,
                canWrite = !g.isArchived && !g.isPendingDeletion,
                rows = items.items.map { t -> t.toRow() },
                total = items.total,
                hasMore = fetched < serverTotal,
                isLoadingMore = false,
                loadMoreFailed = false,
            )
        }
        hasLoaded = true
        null
    }

    private fun EnvelopeTransfer.toRow() = GroupTransferRow(
        transfer = this,
        from = envelopeById[fromEnvelopeId],
        to = envelopeById[toEnvelopeId],
        date = createdAt.toLocalDateTime(zone).date,
    )

    private fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        _state.update { it.copy(isLoadingMore = true, loadMoreFailed = false) }
        loadMoreJob = viewModelScope.launch {
            transfers.getTransfers(groupId, fetched, PAGE_SIZE)
                .onSuccess { page ->
                    fetched += page.items.size
                    serverTotal = page.total
                    _state.update {
                        // An empty page ends the list even if the total said otherwise (rows voided meanwhile)
                        val more = page.items.isNotEmpty() && fetched < serverTotal
                        it.copy(rows = it.rows + page.items.map { t -> t.toRow() }, hasMore = more, isLoadingMore = false)
                    }
                }
                .onFailure { _state.update { it.copy(isLoadingMore = false, loadMoreFailed = true) } }
        }
    }

    /** No confirmation: the row goes at once; `voided_at` is written when the snackbar ends without Undo. */
    private fun delete(transferId: String) {
        val current = _state.value
        if (!current.canWrite) return
        val row = current.rows.firstOrNull { it.transfer.id == transferId && it.canDelete } ?: return
        undo.commit()
        _state.update { it.copy(rows = it.rows - row, total = (it.total - 1).coerceAtLeast(0), actionError = null) }
        undo.show(
            UndoableAction(
                message = Res.string.transfer_deleted,
                argument = "",
                amountCents = row.transfer.amountCents,
                commit = {
                    transfers.voidTransfer(transferId).onSuccess {
                        // The database no longer has the row, so the next page starts one row earlier
                        fetched = (fetched - 1).coerceAtLeast(0)
                        serverTotal = (serverTotal - 1).coerceAtLeast(0)
                    }
                },
                undo = { Result.success(Unit) },
                restore = {
                    _state.update {
                        it.copy(
                            rows = (it.rows + row).sortedWith(
                                compareByDescending<GroupTransferRow> { r -> r.transfer.createdAt }.thenBy { r -> r.transfer.id },
                            ),
                            total = it.total + 1,
                        )
                    }
                },
            ),
        )
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
