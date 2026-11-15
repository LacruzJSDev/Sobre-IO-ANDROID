package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

const val MAX_TAG_NAME_LENGTH = 50

data class MovementFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val saved: Boolean = false,
    val submitError: AppError? = null,
    val currencyCode: String = "EUR",
    /** The group's today: the last day the date can be. */
    val today: LocalDate? = null,

    val kind: EntryKind = EntryKind.Expense,
    /** Text of the amount field (> 0). */
    val amount: String = "",
    val amountError: StringResource? = null,
    /** The amount the movement had when loaded, to tell the user that a higher one is not covered. */
    val storedAmountCents: Long = 0,

    /** Live accounts to choose from (while editing, the ones of the stored account's scope). */
    val accounts: List<Account> = emptyList(),
    /** The account of an expense or income, or the source of a transfer. */
    val accountId: String? = null,
    val accountError: StringResource? = null,
    /** Live envelopes, "Unassigned" (kind System) included; while editing the stored one too, even if archived. */
    val envelopes: List<Envelope> = emptyList(),
    /** Null while nothing is chosen; the id of the System envelope is "Unassigned". */
    val envelopeId: String? = null,
    val envelopeError: StringResource? = null,
    val coverEnvelopeId: String? = null,

    val toAccountId: String? = null,
    val toAccountError: StringResource? = null,
    val crossGroup: Boolean = false,
    /** Other live groups with the same currency, to send a transfer to. */
    val groups: List<Group> = emptyList(),
    val destGroupId: String? = null,
    val destGroupError: StringResource? = null,
    val destAccounts: List<Account> = emptyList(),
    val destAccountId: String? = null,
    val destAccountError: StringResource? = null,
    val isLoadingDestination: Boolean = false,

    val date: LocalDate? = null,
    val notes: String = "",

    val tags: List<Tag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val newTagOpen: Boolean = false,
    val newTagName: String = "",
    val newTagError: StringResource? = null,
    val newTagFailure: AppError? = null,
    val isCreatingTag: Boolean = false,
) {
    val selectedAccount: Account? get() = accounts.firstOrNull { it.id == accountId }

    /** An account not chosen yet is assumed to be inside the envelopes, so the envelope tile shows. */
    val accountInEnvelopes: Boolean get() = selectedAccount?.inEnvelopes ?: true

    val isTransfer: Boolean get() = kind == EntryKind.Transfer

    val showsEnvelope: Boolean get() = !isTransfer && accountInEnvelopes

    /** The "Unassigned" envelope, if the group's list has it. */
    val unassigned: Envelope? get() = envelopes.firstOrNull { it.kind == EnvelopeKind.System }

    val chosenEnvelope: Envelope? get() = envelopes.firstOrNull { it.id == envelopeId }

    private val amountCents: Long? get() = parseMoneyInput(amount)?.takeIf { it > 0 }

    /** Creating an expense whose envelope has less than the amount: the rest is covered from Unassigned. */
    val showsCoverNote: Boolean
        get() {
            val envelope = chosenEnvelope ?: return false
            val cents = amountCents ?: return false
            return !isEditing && kind == EntryKind.Expense && accountInEnvelopes &&
                envelope.kind != EnvelopeKind.System && cents > envelope.balanceCents
        }

    /** Envelopes that can also cover: live, with money, not the chosen one and not "Unassigned". */
    val coverCandidates: List<Envelope>
        get() = envelopes.filter { it.kind != EnvelopeKind.System && !it.isArchived && it.id != envelopeId && it.balanceCents > 0 }

    /** Editing an expense to a higher amount: the database applies no cover on edit. */
    val showsEditExtraNote: Boolean
        get() = isEditing && kind == EntryKind.Expense && (amountCents ?: 0) > storedAmountCents

    /** A transfer between the two sides of the envelope boundary moves its money through Unassigned. */
    val passesThroughUnassigned: Boolean
        get() {
            if (!isTransfer || crossGroup) return false
            val from = selectedAccount ?: return false
            val to = accounts.firstOrNull { it.id == toAccountId } ?: return false
            return from.inEnvelopes != to.inEnvelopes
        }
}
