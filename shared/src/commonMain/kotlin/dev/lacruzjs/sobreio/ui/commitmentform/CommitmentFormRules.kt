package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentChange
import dev.lacruzjs.sobreio.domain.model.CommitmentDraft
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.LinkedCommitmentDraft
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_end_date_before_start
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.generated.resources.validation_funded_needs_bills
import dev.lacruzjs.sobreio.generated.resources.validation_funded_no_bills
import dev.lacruzjs.sobreio.ui.format.formatMoneyInput
import dev.lacruzjs.sobreio.ui.validation.validateName
import dev.lacruzjs.sobreio.ui.validation.validatePositiveAmount
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/*
 * The commitment form's rules, as pure functions of its state: how a choice changes the other fields, how a stored version
 * fills the form, what Save finds wrong and what it sends. The ViewModel only calls them and talks to the repositories.
 */

// Choices ---------------------------------------------------------------------------------------------------------------

/**
 * Moving the commitment to [chosen] (editing only): a transfer cannot go to its own source, and an account outside the
 * envelopes has no envelope and no reserve.
 */
internal fun CommitmentFormUiState.withAccount(chosen: Account): CommitmentFormUiState = copy(
    accountId = chosen.id,
    toAccountId = toAccountId.takeUnless { it == chosen.id },
    envelopeId = if (chosen.inEnvelopes) envelopeId else null,
    funded = funded && chosen.inEnvelopes,
    envelopeError = null,
    toAccountError = null,
)

/** Turning the reserve on keeps the envelope only when it is a Bills one: the list no longer offers any other. */
internal fun CommitmentFormUiState.withFunded(value: Boolean): CommitmentFormUiState {
    val chosenIsBills = envelopes.firstOrNull { it.id == envelopeId }?.purpose == EnvelopePurpose.Bills
    return copy(
        funded = value,
        envelopeId = if (value && !chosenIsBills) null else envelopeId,
        envelopeError = null,
    )
}

/**
 * The form filled from a stored [version] of [commitment]. An expense's envelope that is no longer a choice (archived, or
 * the hidden "Outside envelopes") is left empty, to be picked again.
 */
internal fun CommitmentFormUiState.fromVersion(
    commitment: Commitment,
    version: CommitmentVersion,
    liveEnvelopes: List<Envelope>,
): CommitmentFormUiState {
    val envelope = when (version.kind) {
        CommitmentKind.Expense -> version.envelopeId?.takeIf { id -> accountInEnvelopes && liveEnvelopes.any { it.id == id } }
        else -> version.envelopeId
    }
    return copy(
        isLoading = false,
        name = commitment.name,
        kind = version.kind,
        amount = formatMoneyInput(version.amountCents),
        toAccountId = version.toAccountId,
        envelopeId = envelope,
        firstDueDate = version.firstDueDate,
        isRecurring = version.isRecurring,
        everyN = version.everyN ?: 1,
        unit = version.unit ?: FrequencyUnit.Month,
        endDate = version.endDate,
        autoRegister = version.autoRegister,
        funded = version.funded,
        notes = version.notes.orEmpty(),
    )
}

// Validation ------------------------------------------------------------------------------------------------------------

/** What Save finds wrong, one message per field; [isValid] when there is nothing to fix. */
internal data class CommitmentFormErrors(
    val name: StringResource? = null,
    val amount: StringResource? = null,
    val toAccount: StringResource? = null,
    val envelope: StringResource? = null,
    val destGroup: StringResource? = null,
    val destAccount: StringResource? = null,
    val endDate: StringResource? = null,
) {
    val isValid: Boolean
        get() = listOf(name, amount, toAccount, envelope, destGroup, destAccount, endDate).all { it == null }
}

/**
 * Checks the form before Save. [name] is the trimmed name and [cents] the amount as `parseMoneyInput` read it.
 * [rulesTouched]: a rule field changed since the load, so a reserve's envelope is checked again (a name-only edit of an
 * old commitment is not held back by it). A contribution being edited only checks name and amount: the rest is locked.
 */
internal fun validateCommitmentForm(
    s: CommitmentFormUiState,
    name: String,
    cents: Long?,
    rulesTouched: Boolean,
): CommitmentFormErrors {
    val rules = !s.isLinkedEdit
    val creatingContribution = !s.isEditing && s.isContribution
    // A reserve needs a Bills envelope
    val fundedEnvelopeError = if (rules && s.fundedEffective && (!s.isEditing || rulesTouched)) {
        when {
            !s.hasBillsEnvelope -> Res.string.validation_funded_no_bills
            s.envelopes.firstOrNull { it.id == s.envelopeId }?.purpose != EnvelopePurpose.Bills ->
                Res.string.validation_funded_needs_bills
            else -> null
        }
    } else null
    return CommitmentFormErrors(
        name = validateName(name, MAX_COMMITMENT_NAME_LENGTH),
        amount = validatePositiveAmount(cents),
        toAccount = when {
            !rules || s.kind != CommitmentKind.Transfer -> null
            s.toAccountId == null -> Res.string.validation_destination_account
            s.toAccountId == s.accountId -> Res.string.validation_accounts_differ
            else -> null
        },
        envelope = fundedEnvelopeError ?: if (rules && s.showsExpenseEnvelope && s.envelopeId == null) {
            Res.string.validation_envelope_required
        } else null,
        destGroup = if (creatingContribution && s.destGroupId == null) Res.string.validation_destination_group else null,
        destAccount = if (creatingContribution && s.destGroupId != null && s.destAccountId == null) {
            Res.string.validation_destination_account
        } else null,
        endDate = if (rules && s.isRecurring && s.endDate != null && s.firstDueDate != null && s.endDate < s.firstDueDate) {
            Res.string.validation_end_date_before_start
        } else null,
    )
}

/** The state showing [errors] under their fields. */
internal fun CommitmentFormUiState.withErrors(errors: CommitmentFormErrors): CommitmentFormUiState = copy(
    nameError = errors.name,
    amountError = errors.amount,
    toAccountError = errors.toAccount,
    envelopeError = errors.envelope,
    destGroupError = errors.destGroup,
    destAccountError = errors.destAccount,
    endDateError = errors.endDate,
)

// What Save sends -------------------------------------------------------------------------------------------------------

/** A new commitment of this group. Fields that do not apply to the kind are left out; no end date unless it repeats. */
internal fun commitmentDraftOf(s: CommitmentFormUiState, name: String, cents: Long, firstDueDate: LocalDate) = CommitmentDraft(
    name = name,
    kind = s.kind,
    amountCents = cents,
    accountId = s.accountId,
    firstDueDate = firstDueDate,
    toAccountId = s.toAccountId.takeIf { s.kind == CommitmentKind.Transfer },
    envelopeId = s.envelopeId.takeIf { s.showsExpenseEnvelope },
    isRecurring = s.isRecurring,
    everyN = s.everyN,
    unit = s.unit,
    endDate = s.endDate.takeIf { s.isRecurring },
    funded = s.fundedEffective,
    autoRegister = s.autoRegister,
    notes = s.notes.trim().ifEmpty { null },
)

/** A new contribution to another group: both halves at once (`create_linked_commitment`, which takes no notes). */
internal fun linkedCommitmentDraftOf(s: CommitmentFormUiState, name: String, cents: Long, firstDueDate: LocalDate) =
    LinkedCommitmentDraft(
        name = name,
        outAccountId = s.accountId,
        inAccountId = s.destAccountId.orEmpty(),
        amountCents = cents,
        firstDueDate = firstDueDate,
        isRecurring = s.isRecurring,
        everyN = s.everyN,
        unit = s.unit,
        endDate = s.endDate.takeIf { s.isRecurring },
        outEnvelopeId = s.envelopeId,
        inEnvelopeId = s.destEnvelopeId,
        funded = s.fundedEffective,
        autoRegister = s.autoRegister,
    )

/** The new rules of an edited commitment, from the chosen "Apply from" day. */
internal fun commitmentChangeOf(s: CommitmentFormUiState, cents: Long, firstDueDate: LocalDate) = CommitmentChange(
    amountCents = cents,
    accountId = s.accountId,
    firstDueDate = firstDueDate,
    toAccountId = s.toAccountId.takeIf { s.kind == CommitmentKind.Transfer },
    envelopeId = s.envelopeId.takeIf { s.showsExpenseEnvelope },
    isRecurring = s.isRecurring,
    everyN = s.everyN,
    unit = s.unit,
    endDate = s.endDate.takeIf { s.isRecurring },
    funded = s.fundedEffective,
    autoRegister = s.autoRegister,
    notes = s.notes.trim().ifEmpty { null },
    effectiveFrom = s.effectiveFrom,
)
