package dev.lacruzjs.sobreio.ui.envelopes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_deleted
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_source_invalid
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** The envelopes tab of one group. Currency and role come from the group the Route has already loaded. */
class EnvelopesViewModel(
    private val groupId: String,
    private val envelopes: EnvelopeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopesUiState())
    val state: StateFlow<EnvelopesUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    fun onEvent(event: EnvelopesEvent) {
        when (event) {
            is EnvelopesEvent.GroupLoaded ->
                _state.update { it.copy(currencyCode = event.currencyCode, canEdit = event.canEdit) }
            EnvelopesEvent.Refresh -> {
                // A delete still waiting for its snackbar must be final before the list is read again
                undo.commit()
                load()
            }
            EnvelopesEvent.NewEnvelopeClicked ->
                if (_state.value.canEdit) _state.update { it.openForm(EnvelopesSheet.Create) }
            is EnvelopesEvent.AssignClicked -> openAssign(event.envelopeId)
            is EnvelopesEvent.EditClicked -> edit(event.envelopeId)
            is EnvelopesEvent.DeleteClicked -> delete(event.envelopeId)
            EnvelopesEvent.UndoClicked -> undo.undo()
            EnvelopesEvent.UndoExpired -> undo.commit()
            EnvelopesEvent.SheetDismissed ->
                _state.update { if (it.isSubmitting) it else it.closeSheet() }
            is EnvelopesEvent.NameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is EnvelopesEvent.KindSelected -> _state.update { it.copy(kind = event.kind) }
            is EnvelopesEvent.MonthlyTargetChanged ->
                _state.update { it.copy(monthlyTarget = event.value, monthlyTargetError = null, submitError = null) }
            is EnvelopesEvent.AmountChanged ->
                _state.update { it.copy(amount = event.value, amountError = null, submitError = null) }
            is EnvelopesEvent.SourceSelected ->
                _state.update { it.copy(sourceId = event.envelopeId, sourceError = null, submitError = null) }
            EnvelopesEvent.ConfirmClicked -> confirm()
            EnvelopesEvent.AssignConfirmed -> assign()
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    private fun load() {
        viewModelScope.launch {
            // A new target becomes an assignment of the current period; a failure here must not stop the list
            envelopes.materializeThroughToday(groupId)
            val list = envelopes.getEnvelopes(groupId)
            val summary = envelopes.getSummary(groupId)
            val failure = list.exceptionOrNull() ?: summary.exceptionOrNull()
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
            } else {
                val all = list.getOrThrow()
                val figures = summary.getOrThrow()
                _state.update {
                    it.copy(
                        envelopes = all.filter { e -> e.kind != EnvelopeKind.System }.sortedBy { e -> e.name.lowercase() },
                        unassigned = all.firstOrNull { e -> e.kind == EnvelopeKind.System },
                        totalCents = figures.totalCents,
                        unassignedCents = figures.unassignedCents,
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    private fun edit(envelopeId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val envelope = current.envelopes.firstOrNull { it.id == envelopeId } ?: return
        _state.update {
            it.openForm(EnvelopesSheet.Edit).copy(
                editingEnvelope = envelope,
                name = envelope.name,
                kind = envelope.kind,
                monthlyTarget = envelope.monthlyTargetCents.toInputText(),
            )
        }
    }

    private fun openAssign(envelopeId: String) {
        val current = _state.value
        val envelope = current.envelopes.firstOrNull { it.id == envelopeId } ?: return
        _state.update {
            it.closeSheet().copy(
                sheet = EnvelopesSheet.Assign,
                assignTarget = envelope,
                sourceId = current.unassigned?.id,
            )
        }
    }

    private fun confirm() {
        val current = _state.value
        if (!current.canEdit || current.isSubmitting) return
        if (current.sheet != EnvelopesSheet.Create && current.sheet != EnvelopesSheet.Edit) return
        val name = current.name.trim()
        val nameError = validateEnvelopeName(name)
        val target = parseMoneyInput(current.monthlyTarget)
        val targetError = when {
            target == null -> Res.string.validation_amount_invalid
            target < 0 -> Res.string.validation_amount_negative
            else -> null
        }
        if (nameError != null || targetError != null || target == null) {
            _state.update { it.copy(nameError = nameError, monthlyTargetError = targetError) }
            return
        }
        val editing = current.sheet == EnvelopesSheet.Edit
        val editingEnvelope = current.editingEnvelope
        if (editing && editingEnvelope == null) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (editingEnvelope != null) {
                envelopes.updateEnvelope(editingEnvelope.id, name, current.kind, target)
            } else {
                envelopes.createEnvelope(groupId, name, current.kind, target).map { }
            }
            result
                .onSuccess {
                    _state.update { it.closeSheet() }
                    load()
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    private fun assign() {
        val current = _state.value
        if (current.isSubmitting || current.sheet != EnvelopesSheet.Assign) return
        val target = current.assignTarget ?: return
        val amount = parseMoneyInput(current.amount)
        val amountError = when {
            amount == null -> Res.string.validation_amount_invalid
            amount <= 0 -> Res.string.validation_amount_positive
            else -> null
        }
        val sourceId = current.sourceId
        val sourceError = if (sourceId == null || sourceId == target.id) Res.string.validation_source_invalid else null
        if (amountError != null || sourceError != null || amount == null || sourceId == null) {
            _state.update { it.copy(amountError = amountError, sourceError = sourceError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            envelopes.assign(groupId, sourceId, target.id, amount)
                .onSuccess {
                    _state.update { it.closeSheet() }
                    load()
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /**
     * No confirmation: the envelope disappears at once and is archived right away, so a refusal (balance not zero,
     * live commitments) comes back immediately and the row returns. The snackbar's "Undo" unarchives it.
     */
    private fun delete(envelopeId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val envelope = current.envelopes.firstOrNull { it.id == envelopeId } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update { it.copy(envelopes = it.envelopes - envelope, actionError = null) }
        val restore = {
            _state.update { it.copy(envelopes = (it.envelopes + envelope).sortedBy { e -> e.name.lowercase() }) }
            load()
        }
        viewModelScope.launch {
            envelopes.archiveEnvelope(envelope.id)
                .onSuccess {
                    undo.show(
                        UndoableAction(
                            message = Res.string.envelope_deleted,
                            argument = envelope.name,
                            commit = { Result.success(Unit) },
                            undo = { envelopes.unarchiveEnvelope(envelope.id) },
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

    private fun validateEnvelopeName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_ENVELOPE_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }

    private fun EnvelopesUiState.openForm(sheet: EnvelopesSheet) = closeSheet().copy(sheet = sheet)

    /** Closes any sheet and clears every field of the forms. */
    private fun EnvelopesUiState.closeSheet() = copy(
        sheet = EnvelopesSheet.None,
        editingEnvelope = null,
        name = "",
        nameError = null,
        kind = EnvelopeKind.Spending,
        monthlyTarget = "",
        monthlyTargetError = null,
        assignTarget = null,
        amount = "",
        amountError = null,
        sourceId = null,
        sourceError = null,
        isSubmitting = false,
        submitError = null,
    )

    /** Cents as the text the user would type: nothing for no target, otherwise `12,50` style with a plain point. */
    private fun Long.toInputText(): String = when {
        this <= 0 -> ""
        this % 100 == 0L -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }
}
