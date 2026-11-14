package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.Group
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

const val MAX_COMMITMENT_NAME_LENGTH = 100
const val MIN_EVERY_N = 1
const val MAX_EVERY_N = 99

data class CommitmentFormUiState(
    val name: String = "",
    val nameError: StringResource? = null,
    /** The kind to create; while editing it is the kind of the commitment and cannot change. [CommitmentKind.GroupTransferOut] is a contribution. */
    val kind: CommitmentKind = CommitmentKind.Expense,
    /** Text of the amount field (> 0). */
    val amount: String = "",
    val amountError: StringResource? = null,
    val currencyCode: String = "EUR",

    // Money
    /** The chosen (source) account: the detail's account when creating; while editing it can be moved to another one. */
    val accountId: String = "",
    /** The account the commitment had when it was loaded; [accountId] differing from it is a move. */
    val storedAccountId: String = "",
    /** The group's live accounts: the choices of the account and, minus the chosen one, of a transfer's destination. */
    val accounts: List<Account> = emptyList(),
    val toAccountId: String? = null,
    val toAccountError: StringResource? = null,
    /** The group's live spending and saving envelopes. */
    val envelopes: List<Envelope> = emptyList(),
    /** Envelope of an expense, or the source envelope of a contribution; null is "Unassigned" for a contribution. */
    val envelopeId: String? = null,
    val envelopeError: StringResource? = null,

    // Contribution to another group
    /** Other live groups of the same currency where the user is owner or admin. */
    val groups: List<Group> = emptyList(),
    val destGroupId: String? = null,
    val destGroupName: String? = null,
    val destGroupError: StringResource? = null,
    val destAccounts: List<Account> = emptyList(),
    val destAccountId: String? = null,
    val destAccountError: StringResource? = null,
    val destEnvelopes: List<Envelope> = emptyList(),
    /** Envelope in the destination group; null is "Unassigned". */
    val destEnvelopeId: String? = null,
    val isLoadingDestination: Boolean = false,

    // Calendar
    val firstDueDate: LocalDate? = null,
    val isRecurring: Boolean = true,
    val everyN: Int = 1,
    val unit: FrequencyUnit = FrequencyUnit.Month,
    val endDate: LocalDate? = null,
    val endDateError: StringResource? = null,

    // Registration
    val autoRegister: Boolean = false,
    val funded: Boolean = false,
    val notes: String = "",

    // Editing
    val isEditing: Boolean = false,
    /** The group's today (from the database): the default of [effectiveFrom] and of the first due date. */
    val today: LocalDate? = null,
    /** The day the new rules start applying; earlier due dates keep the old ones. */
    val effectiveFrom: LocalDate? = null,
    /** Editing a contribution: only the name, the amount and the date change; the rest belongs to both groups. */
    val isLinkedEdit: Boolean = false,

    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** The commitment was created or saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
) {
    val isContribution: Boolean get() = kind == CommitmentKind.GroupTransferOut

    val accountName: String get() = accounts.firstOrNull { it.id == accountId }?.name.orEmpty()

    /** The account shares its money into the envelopes; an expense of one that does not goes to "Outside envelopes" by itself. */
    val accountInEnvelopes: Boolean get() = accounts.firstOrNull { it.id == accountId }?.inEnvelopes ?: true

    /** The destinations of a transfer: every live account but the chosen one. */
    val destinationAccounts: List<Account> get() = accounts.filter { it.id != accountId }

    /** Editing a commitment (not a contribution): the account can be changed. */
    val canChangeAccount: Boolean get() = isEditing && !isLinkedEdit

    /** The account was moved: the form explains from when the new one is used. */
    val accountMoved: Boolean get() = canChangeAccount && storedAccountId.isNotEmpty() && accountId != storedAccountId

    /** The envelope choice of an expense: an account outside the envelopes has none (the database uses "Outside envelopes"). */
    val showsExpenseEnvelope: Boolean get() = kind == CommitmentKind.Expense && accountInEnvelopes

    /**
     * The database only funds an expense with an envelope that is inside the envelopes, or a contribution with a source
     * envelope, and only one-off or monthly and yearly ones (weekly and daily go in the envelope's monthly target).
     */
    val canFund: Boolean
        get() {
            val hasEnvelope = when (kind) {
                CommitmentKind.Expense -> accountInEnvelopes && envelopeId != null
                CommitmentKind.GroupTransferOut -> envelopeId != null
                else -> false
            }
            return hasEnvelope && (!isRecurring || unit == FrequencyUnit.Month || unit == FrequencyUnit.Year)
        }

    /** What is sent: a switch left on that no longer applies is off. */
    val fundedEffective: Boolean get() = funded && canFund

    /** Every field but name, amount and date is read only while editing a contribution. */
    val fieldsLocked: Boolean get() = isLinkedEdit
}

sealed interface CommitmentFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : CommitmentFormEvent
    data class NameChanged(val value: String) : CommitmentFormEvent
    data class KindSelected(val kind: CommitmentKind) : CommitmentFormEvent
    data class AmountChanged(val value: String) : CommitmentFormEvent
    data class AccountSelected(val id: String) : CommitmentFormEvent
    data class ToAccountSelected(val id: String) : CommitmentFormEvent
    data class EnvelopeSelected(val id: String?) : CommitmentFormEvent
    data class DestGroupSelected(val id: String) : CommitmentFormEvent
    data class DestAccountSelected(val id: String) : CommitmentFormEvent
    data class DestEnvelopeSelected(val id: String?) : CommitmentFormEvent
    data class FirstDueDateChanged(val date: LocalDate) : CommitmentFormEvent
    data class RecurringChanged(val value: Boolean) : CommitmentFormEvent
    data class EveryChanged(val value: Int) : CommitmentFormEvent
    data class UnitSelected(val unit: FrequencyUnit) : CommitmentFormEvent
    data class EndDateChanged(val date: LocalDate?) : CommitmentFormEvent
    data class AutoRegisterChanged(val value: Boolean) : CommitmentFormEvent
    data class FundedChanged(val value: Boolean) : CommitmentFormEvent
    data class NotesChanged(val value: String) : CommitmentFormEvent
    data class EffectiveFromChanged(val date: LocalDate) : CommitmentFormEvent
    data object SaveClicked : CommitmentFormEvent
    data object Retry : CommitmentFormEvent
    data object NavigationHandled : CommitmentFormEvent
}
