package dev.lacruzjs.sobreio.ui.envelopeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

const val MAX_ENVELOPE_NAME_LENGTH = 100

data class EnvelopeFormUiState(
    val name: String = "",
    val nameError: StringResource? = null,
    val kind: EnvelopeKind = EnvelopeKind.Spending,
    /** Text of the monthly target field; blank means no target. */
    val monthlyTarget: String = "",
    val monthlyTargetError: StringResource? = null,
    /** Editing: the envelope is being read. */
    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** The envelope was created or saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
)

sealed interface EnvelopeFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : EnvelopeFormEvent
    data class NameChanged(val value: String) : EnvelopeFormEvent
    data class KindSelected(val kind: EnvelopeKind) : EnvelopeFormEvent
    data class MonthlyTargetChanged(val value: String) : EnvelopeFormEvent
    data object SaveClicked : EnvelopeFormEvent
    data object Retry : EnvelopeFormEvent
    data object NavigationHandled : EnvelopeFormEvent
}

/** The create and edit form of an envelope: name, kind and monthly target. [envelopeId] null creates. */
class EnvelopeFormViewModel(
    private val groupId: String,
    private val envelopeId: String?,
    private val envelopes: EnvelopeRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeFormUiState(isLoading = envelopeId != null))
    val state: StateFlow<EnvelopeFormUiState> = _state.asStateFlow()

    init {
        if (envelopeId != null) load(envelopeId)
    }

    fun onEvent(event: EnvelopeFormEvent) {
        when (event) {
            is EnvelopeFormEvent.NameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is EnvelopeFormEvent.KindSelected -> _state.update { it.copy(kind = event.kind) }
            is EnvelopeFormEvent.MonthlyTargetChanged ->
                _state.update { it.copy(monthlyTarget = event.value, monthlyTargetError = null, submitError = null) }
            EnvelopeFormEvent.SaveClicked -> save()
            EnvelopeFormEvent.Retry -> envelopeId?.let { load(it) }
            EnvelopeFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            EnvelopeFormEvent.CloseClicked -> Unit
        }
    }

    private fun load(id: String) {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            envelopes.getEnvelopes(groupId)
                .onSuccess { list ->
                    val envelope = list.firstOrNull { it.id == id && it.kind != EnvelopeKind.System }
                    if (envelope == null) {
                        _state.update { it.copy(isLoading = false, loadError = AppError.NotFound) }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                name = envelope.name,
                                kind = envelope.kind,
                                monthlyTarget = envelope.monthlyTargetCents.toInputText(),
                            )
                        }
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.asAppError()) } }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading || current.loadError != null) return
        val name = current.name.trim()
        val nameError = validateName(name)
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
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (envelopeId != null) {
                envelopes.updateEnvelope(envelopeId, name, current.kind, target)
            } else {
                envelopes.createEnvelope(groupId, name, current.kind, target).map { }
            }
            result
                .onSuccess { _state.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    private fun validateName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_ENVELOPE_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }

    /** Cents as the text the user would type: nothing for no target, otherwise `12.50` style with a plain point. */
    private fun Long.toInputText(): String = when {
        this <= 0 -> ""
        this % 100 == 0L -> (this / 100).toString()
        else -> "${this / 100}.${(this % 100).toString().padStart(2, '0')}"
    }
}
