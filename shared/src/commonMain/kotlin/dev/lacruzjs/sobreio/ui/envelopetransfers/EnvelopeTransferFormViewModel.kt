package dev.lacruzjs.sobreio.ui.envelopetransfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferDraft
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_envelopes_differ
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class EnvelopeTransferFormUiState(
    /** Editing an existing move rather than recording a new one. */
    val isEditing: Boolean = false,
    /** Origin of the move being edited; a new one is manual. */
    val origin: TransferOrigin = TransferOrigin.Manual,
    val amount: String = "",
    val amountError: StringResource? = null,
    val fromId: String? = null,
    val fromError: StringResource? = null,
    val toId: String? = null,
    val toError: StringResource? = null,
    val notes: String = "",
    /** The envelopes to choose from: "Unassigned" first, then the live ones by name (and the stored ones when archived). */
    val envelopes: List<Envelope> = emptyList(),
    val currencyCode: String = "EUR",
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** Saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
) {
    /** An automatic move keeps its envelopes: only the amount and the notes change. */
    val envelopesLocked: Boolean get() = !origin.canChangeEnvelopes
}

sealed interface EnvelopeTransferFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : EnvelopeTransferFormEvent
    data class AmountChanged(val value: String) : EnvelopeTransferFormEvent
    data class FromSelected(val envelopeId: String) : EnvelopeTransferFormEvent
    data class ToSelected(val envelopeId: String) : EnvelopeTransferFormEvent
    data class NotesChanged(val value: String) : EnvelopeTransferFormEvent
    data object SaveClicked : EnvelopeTransferFormEvent
    data object Retry : EnvelopeTransferFormEvent
    data object NavigationHandled : EnvelopeTransferFormEvent
}

/**
 * Records a manual move between envelopes ([transferId] null) or edits a live one (D-16): amount, notes and, for a manual
 * move, both envelopes. A monthly target keeps "Unassigned" and its envelope.
 */
class EnvelopeTransferFormViewModel(
    private val groupId: String,
    private val transferId: String?,
    /** A new move opened from one envelope's moves starts with that envelope as the destination. */
    private val initialToId: String? = null,
    private val groups: GroupRepository,
    private val envelopes: EnvelopeRepository,
    private val transfers: EnvelopeTransferRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeTransferFormUiState(isEditing = transferId != null))
    val state: StateFlow<EnvelopeTransferFormUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: EnvelopeTransferFormEvent) {
        when (event) {
            is EnvelopeTransferFormEvent.AmountChanged ->
                _state.update { it.copy(amount = event.value, amountError = null, submitError = null) }
            is EnvelopeTransferFormEvent.FromSelected -> _state.update {
                if (it.envelopesLocked) it else it.copy(fromId = event.envelopeId, fromError = null, toError = null, submitError = null)
            }
            is EnvelopeTransferFormEvent.ToSelected -> _state.update {
                if (it.envelopesLocked) it else it.copy(toId = event.envelopeId, toError = null, submitError = null)
            }
            is EnvelopeTransferFormEvent.NotesChanged -> _state.update { it.copy(notes = event.value) }
            EnvelopeTransferFormEvent.SaveClicked -> save()
            EnvelopeTransferFormEvent.Retry -> load()
            EnvelopeTransferFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            EnvelopeTransferFormEvent.CloseClicked -> Unit
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val failure = coroutineScope {
                val group = async { groups.getGroup(groupId) }
                val list = async { envelopes.getEnvelopes(groupId) }
                val stored = async { transferId?.let { transfers.getTransfer(it) } }
                val listResult = list.await()
                val storedResult = stored.await()
                val groupResult = group.await()
                (groupResult.exceptionOrNull() ?: listResult.exceptionOrNull() ?: storedResult?.exceptionOrNull())
                    ?.let { return@coroutineScope it.asAppError() }
                val all = listResult.getOrThrow()
                val transfer = storedResult?.getOrThrow()
                if (transfer != null && !transfer.origin.canEdit) return@coroutineScope AppError.NotFound
                val unassigned = all.firstOrNull { it.kind == EnvelopeKind.System }
                // Archived envelopes cannot receive or give money; the stored ones still show their name
                val keep = setOfNotNull(transfer?.fromEnvelopeId, transfer?.toEnvelopeId)
                val choices = listOfNotNull(unassigned) +
                    all.filter { it.kind != EnvelopeKind.System && (!it.isArchived || it.id in keep) }
                _state.update {
                    it.copy(
                        isLoading = false,
                        currencyCode = groupResult.getOrThrow().currencyCode,
                        envelopes = choices,
                        origin = transfer?.origin ?: TransferOrigin.Manual,
                        amount = transfer?.amountCents?.toInputText() ?: it.amount,
                        // A new move starts from "Unassigned", like the Assign sheet
                        fromId = transfer?.fromEnvelopeId ?: it.fromId ?: unassigned?.id,
                        toId = transfer?.toEnvelopeId ?: it.toId ?: initialToId?.takeIf { id -> choices.any { e -> e.id == id && e.id != unassigned?.id } },
                        notes = transfer?.notes ?: it.notes,
                    )
                }
                null
            }
            if (failure != null) _state.update { it.copy(isLoading = false, loadError = failure) }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading || current.loadError != null) return
        val amount = parseMoneyInput(current.amount)
        val amountError = when {
            amount == null -> Res.string.validation_amount_invalid
            amount <= 0 -> Res.string.validation_amount_positive
            else -> null
        }
        val fromError = if (current.fromId == null) Res.string.validation_required else null
        val toError = when {
            current.toId == null -> Res.string.validation_required
            current.toId == current.fromId -> Res.string.validation_envelopes_differ
            else -> null
        }
        val from = current.fromId
        val to = current.toId
        if (amountError != null || fromError != null || toError != null || amount == null || from == null || to == null) {
            _state.update { it.copy(amountError = amountError, fromError = fromError, toError = toError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val draft = EnvelopeTransferDraft(from, to, amount, current.notes.trim().ifEmpty { null })
            val result = if (transferId != null) transfers.updateTransfer(transferId, draft) else transfers.createTransfer(groupId, draft)
            result
                .onSuccess { _state.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /** Cents as the text the user would type, `12.50` style with a plain point. */
    private fun Long.toInputText(): String = when {
        this % 100 == 0L -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }
}
