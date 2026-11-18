package dev.lacruzjs.sobreio.ui.envelopeform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_type_has_funded
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource

const val MAX_ENVELOPE_NAME_LENGTH = 100

data class EnvelopeFormUiState(
    val name: String = "",
    val nameError: StringResource? = null,
    /** The envelope type (D-46): it decides the fields below and the note at the end of the form. */
    val purpose: EnvelopePurpose = EnvelopePurpose.Monthly,
    /** The database refused to change the type (FZ021): shown under the type choice. */
    val purposeError: StringResource? = null,
    /** Hex of the chosen palette colour; null is the brand ink. */
    val color: String? = null,
    /** Key of the chosen icon of the envelope catalogue; null is none. */
    val icon: String? = null,
    /** Holder: a member's user id, or null for a common envelope. */
    val ownerUserId: String? = null,
    /** Text of the monthly target (Monthly, required) or contribution (Savings, optional) field; unused by Bills and Free. */
    val monthlyTarget: String = "",
    val monthlyTargetError: StringResource? = null,
    /** The active members: they are the holders to choose from. */
    val members: List<Member> = emptyList(),
    /** The group's today and the first day of the next period; null until read (or when the read failed). */
    val today: LocalDate? = null,
    val nextPeriodStart: LocalDate? = null,
    /** Editing: the stored target and first target day (D-53), to tell when the start can still be chosen. */
    val storedTargetCents: Long = 0,
    val storedTargetsFrom: LocalDate? = null,
    /** The user's choice of "Start next period"; null keeps what the envelope has. */
    val startNextPeriodChoice: Boolean? = null,
    /** Editing an existing envelope rather than creating one. */
    val isEditing: Boolean = false,
    /** Editing: the envelope is being read. */
    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** The envelope was created or saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
) {
    /** The holder is only asked for when there is someone to choose between. */
    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()

    /** Monthly and Savings have an amount field; Bills and Free send 0. */
    val showsTarget: Boolean get() = purpose.hasTarget

    /** A monthly envelope cannot exist without its target. */
    val targetRequired: Boolean get() = purpose == EnvelopePurpose.Monthly

    /** The stored target only starts in a later period (D-53). */
    val storedStartsLater: Boolean
        get() = storedTargetsFrom != null && today != null && storedTargetsFrom > today

    /**
     * "Start next period" is offered for a type with a target when the dates are known, and only while no target has
     * been assigned under the stored rule: creating, an envelope that had no target, or one that still starts later.
     * An assignment already made is removed from the envelope detail instead.
     */
    val canChooseStart: Boolean
        get() = showsTarget && nextPeriodStart != null &&
            (!isEditing || storedTargetCents == 0L || storedStartsLater)

    /** The switch as shown: the user's choice, or what the envelope has. */
    val startsNextPeriod: Boolean get() = startNextPeriodChoice ?: storedStartsLater

    /**
     * The first target day to send (D-53): the next period's first day when the switch is on, today when it is turned
     * off on an envelope that started later, otherwise null so the database keeps its own rule.
     */
    fun targetsFromToSend(): LocalDate? = when {
        !canChooseStart -> null
        startsNextPeriod -> nextPeriodStart
        storedStartsLater -> today
        else -> null
    }
}

sealed interface EnvelopeFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : EnvelopeFormEvent
    data class NameChanged(val value: String) : EnvelopeFormEvent
    data class PurposeSelected(val purpose: EnvelopePurpose) : EnvelopeFormEvent
    data class ColorSelected(val hex: String?) : EnvelopeFormEvent
    data class IconSelected(val key: String?) : EnvelopeFormEvent
    data class HolderSelected(val userId: String?) : EnvelopeFormEvent
    data class MonthlyTargetChanged(val value: String) : EnvelopeFormEvent
    /** The "Start next period" switch (D-53). */
    data class StartNextPeriodChanged(val value: Boolean) : EnvelopeFormEvent
    data object SaveClicked : EnvelopeFormEvent
    data object Retry : EnvelopeFormEvent
    data object NavigationHandled : EnvelopeFormEvent
}

/** The create and edit form of an envelope: name, type, holder, colour, icon and, for some types, the monthly amount. [envelopeId] null creates. */
class EnvelopeFormViewModel(
    private val groupId: String,
    private val envelopeId: String?,
    private val envelopes: EnvelopeRepository,
    private val members: MemberRepository,
    private val dates: AccountDetailRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EnvelopeFormUiState(isLoading = envelopeId != null, isEditing = envelopeId != null))
    val state: StateFlow<EnvelopeFormUiState> = _state.asStateFlow()

    init {
        loadMembers()
        loadDates()
        if (envelopeId != null) load(envelopeId)
    }

    fun onEvent(event: EnvelopeFormEvent) {
        when (event) {
            is EnvelopeFormEvent.NameChanged ->
                _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is EnvelopeFormEvent.PurposeSelected ->
                _state.update { it.copy(purpose = event.purpose, purposeError = null, monthlyTargetError = null, submitError = null) }
            is EnvelopeFormEvent.ColorSelected -> _state.update { it.copy(color = event.hex) }
            is EnvelopeFormEvent.IconSelected -> _state.update { it.copy(icon = event.key) }
            is EnvelopeFormEvent.HolderSelected -> _state.update { it.copy(ownerUserId = event.userId) }
            is EnvelopeFormEvent.MonthlyTargetChanged ->
                _state.update { it.copy(monthlyTarget = event.value, monthlyTargetError = null, submitError = null) }
            is EnvelopeFormEvent.StartNextPeriodChanged -> _state.update { it.copy(startNextPeriodChoice = event.value) }
            EnvelopeFormEvent.SaveClicked -> save()
            EnvelopeFormEvent.Retry -> envelopeId?.let { load(it) }
            EnvelopeFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            EnvelopeFormEvent.CloseClicked -> Unit
        }
    }

    /** A failure only hides the holder choice: the form works without it. */
    private fun loadMembers() {
        viewModelScope.launch {
            members.getMembers(groupId).onSuccess { list -> _state.update { it.copy(members = list) } }
        }
    }

    /** A failure only hides the "Start next period" switch: the database then starts the target today. */
    private fun loadDates() {
        viewModelScope.launch {
            val today = dates.getToday(groupId).getOrNull() ?: return@launch
            val period = dates.getCurrentPeriod(groupId).getOrNull() ?: return@launch
            _state.update { it.copy(today = today, nextPeriodStart = period.end.plus(DatePeriod(days = 1))) }
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
                                purpose = envelope.purpose ?: envelope.fallbackPurpose(),
                                color = envelope.color,
                                icon = envelope.icon,
                                ownerUserId = envelope.ownerUserId,
                                monthlyTarget = envelope.monthlyTargetCents.toInputText(),
                                storedTargetCents = envelope.monthlyTargetCents,
                                storedTargetsFrom = envelope.targetsFrom,
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
        // Bills and Free have no target field: the database wants 0 there
        val target = if (current.showsTarget) parseMoneyInput(current.monthlyTarget) else 0L
        val targetError = when {
            target == null -> Res.string.validation_amount_invalid
            target < 0 -> Res.string.validation_amount_negative
            current.targetRequired && target == 0L -> Res.string.validation_amount_positive
            else -> null
        }
        if (nameError != null || targetError != null || target == null) {
            _state.update { it.copy(nameError = nameError, monthlyTargetError = targetError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null, purposeError = null) }
        viewModelScope.launch {
            val draft = EnvelopeDraft(
                name = name,
                purpose = current.purpose,
                monthlyTargetCents = target,
                ownerUserId = current.ownerUserId,
                color = current.color,
                icon = current.icon,
                targetsFrom = current.targetsFromToSend(),
            )
            val result = if (envelopeId != null) {
                envelopes.updateEnvelope(envelopeId, draft)
            } else {
                envelopes.createEnvelope(groupId, draft).map { }
            }
            result
                .onSuccess { _state.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { e -> _state.update { it.failedWith(e.asAppError()) } }
        }
    }

    /** Leaving Bills while funded commitments still point here (FZ021) is explained under the type choice. */
    private fun EnvelopeFormUiState.failedWith(error: AppError): EnvelopeFormUiState =
        if (error is AppError.BusinessRule && error.code == "FZ021") {
            copy(isSubmitting = false, purposeError = Res.string.envelope_type_has_funded)
        } else {
            copy(isSubmitting = false, submitError = error)
        }

    /** Only for a database that sent no type: the same deduction the database makes when a client sends none. */
    private fun Envelope.fallbackPurpose(): EnvelopePurpose = when {
        kind == EnvelopeKind.Saving -> EnvelopePurpose.Savings
        monthlyTargetCents > 0 -> EnvelopePurpose.Monthly
        else -> EnvelopePurpose.Free
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
