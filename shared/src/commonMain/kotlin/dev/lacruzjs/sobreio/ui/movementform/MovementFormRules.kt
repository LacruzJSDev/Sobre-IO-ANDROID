package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.TransactionDraft
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_account_required
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.ui.format.formatMoneyInput
import dev.lacruzjs.sobreio.ui.validation.validatePositiveAmount
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/*
 * The movement form's rules, as pure functions of its state: what Save finds wrong, what it sends, how a prefill opens
 * the form and which tags change. The ViewModel only calls them and talks to the repositories.
 */

/** What Save finds wrong, one message per field; [isValid] when there is nothing to fix. */
internal data class MovementFormErrors(
    val amount: StringResource? = null,
    val account: StringResource? = null,
    val envelope: StringResource? = null,
    val toAccount: StringResource? = null,
    val destGroup: StringResource? = null,
    val destAccount: StringResource? = null,
) {
    val isValid: Boolean
        get() = amount == null && account == null && envelope == null && toAccount == null && destGroup == null && destAccount == null
}

/** Checks the form before Save; [cents] is the amount as `parseMoneyInput` read it (null when it is not an amount). */
internal fun validateMovementForm(s: MovementFormUiState, cents: Long?): MovementFormErrors {
    val crossGroupTransfer = s.isTransfer && s.crossGroup
    return MovementFormErrors(
        amount = validatePositiveAmount(cents),
        account = if (s.accountId == null) Res.string.validation_account_required else null,
        // An expense must say where it is spent from; income goes to Unassigned when none is chosen
        envelope = if (s.showsEnvelope && s.kind == EntryKind.Expense && s.envelopeId == null) {
            Res.string.validation_envelope_required
        } else null,
        toAccount = when {
            !s.isTransfer || s.crossGroup -> null
            s.toAccountId == null -> Res.string.validation_destination_account
            s.toAccountId == s.accountId -> Res.string.validation_accounts_differ
            else -> null
        },
        destGroup = if (crossGroupTransfer && s.destGroupId == null) Res.string.validation_destination_group else null,
        destAccount = if (crossGroupTransfer && s.destGroupId != null && s.destAccountId == null) {
            Res.string.validation_destination_account
        } else null,
    )
}

/** The state showing [errors] under their fields. */
internal fun MovementFormUiState.withErrors(errors: MovementFormErrors): MovementFormUiState = copy(
    amountError = errors.amount,
    accountError = errors.account,
    envelopeError = errors.envelope,
    toAccountError = errors.toAccount,
    destGroupError = errors.destGroup,
    destAccountError = errors.destAccount,
)

/**
 * The expense or income to write. An account outside the envelopes sends no envelope (the database uses its hidden one);
 * inside, no choice means Unassigned. The cover is the chosen envelope's whole balance, only while the cover note shows:
 * the database takes just what is missing.
 */
internal fun transactionDraftOf(s: MovementFormUiState, accountId: String, cents: Long, date: LocalDate): TransactionDraft {
    val envelopeId = if (s.accountInEnvelopes) s.envelopeId ?: s.unassigned?.id else null
    val cover = if (s.showsCoverNote) {
        s.coverEnvelopeId
            ?.let { id -> s.envelopes.firstOrNull { it.id == id } }
            ?.let { listOf(CoverItem(it.id, it.balanceCents)) }
            .orEmpty()
    } else {
        emptyList()
    }
    return TransactionDraft(accountId, s.kind, cents, envelopeId, date, s.notes.trim().ifEmpty { null }, cover)
}

/**
 * A new form opened with [prefill]: only accounts that are live choices are taken (the destination never equals the
 * source), an income starts on Unassigned, and the date stays the group's today.
 */
internal fun MovementFormUiState.withPrefill(prefill: MovementPrefill?): MovementFormUiState {
    val p = prefill ?: return this
    val live = accounts.map { it.id }.toSet()
    val from = p.fromAccountId?.takeIf { it in live }
    val to = p.toAccountId?.takeIf { it in live && it != from }
    return copy(
        kind = p.kind,
        accountId = from ?: accountId.takeIf { p.fromAccountId == null },
        toAccountId = if (p.kind == EntryKind.Transfer) to else null,
        envelopeId = if (p.kind == EntryKind.Income) unassigned?.id else null,
        amount = p.amountCents?.let(::formatMoneyInput).orEmpty(),
    )
}

/** The tags a movement has in the database, and what to add and remove so they match the chosen ones. */
internal class TagSync(stored: Set<String> = emptySet()) {

    /** Tags the movement has in the database now. */
    var stored: Set<String> = stored
        private set

    /** What to insert and delete for [selected]; null when they already match. */
    fun changesFor(selected: Set<String>): TagChanges? {
        val added = selected - stored
        val removed = stored - selected
        return if (added.isEmpty() && removed.isEmpty()) null else TagChanges(added, removed)
    }

    /** The database holds [selected] now. */
    fun synced(selected: Set<String>) {
        stored = selected
    }
}

internal data class TagChanges(val added: Set<String>, val removed: Set<String>)
