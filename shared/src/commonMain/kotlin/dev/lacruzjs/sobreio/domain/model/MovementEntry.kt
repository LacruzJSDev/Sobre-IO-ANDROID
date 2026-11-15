package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/** What the new / edit movement form can record. Adjustments are only made from "Compare with bank". */
enum class EntryKind { Expense, Income, Transfer }

/**
 * One envelope asked to cover what the chosen envelope lacks (`p_cover`). [amountCents] is the most the database may take
 * from it; the database takes only what is needed, in order, and refuses more than the envelope has (FZ019).
 */
data class CoverItem(val envelopeId: String, val amountCents: Long)

/**
 * An expense or income to record or to write over a stored one. [amountCents] is positive; the sign comes from [kind].
 * [envelopeId] null means "Unassigned" when creating (the database resolves it) and "leave as it is" when updating, which
 * is what an account outside the envelopes needs. [cover] only applies to creating an expense.
 */
data class TransactionDraft(
    val accountId: String,
    val kind: EntryKind,
    val amountCents: Long,
    val envelopeId: String?,
    val date: LocalDate,
    val notes: String?,
    val cover: List<CoverItem> = emptyList(),
)

/** A transfer between two accounts of the same group; [amountCents] is positive. */
data class TransferDraft(
    val fromAccountId: String,
    val toAccountId: String,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
)

/** A transfer to an account of another group (`create_group_transfer`); both envelopes are "Unassigned". */
data class GroupTransferDraft(
    val outAccountId: String,
    val inAccountId: String,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
)

/** A stored expense or income, as the form edits it. [amountCents] is positive. */
data class StoredTransaction(
    val id: String,
    val groupId: String,
    val accountId: String,
    val envelopeId: String,
    val kind: EntryKind,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
)

/** A stored account transfer, as the form edits it. */
data class StoredTransfer(
    val id: String,
    val groupId: String,
    val fromAccountId: String,
    val toAccountId: String,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
)
