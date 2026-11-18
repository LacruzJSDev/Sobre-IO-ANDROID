package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/** What a statement row is, as `bank_import_preview` says (D-52). */
enum class BankRowStatus { New, Imported, Future, BeforeOpening }

/** What will be done with a statement row. */
enum class BankRowAction(val wireName: String) {
    Create("create"),
    Confirm("confirm"),
    Link("link"),
    Ignore("ignore"),
}

/** A pending occurrence that a row could confirm; [amountCents] is signed for the account. */
data class PendingCandidate(
    val commitmentId: String,
    val name: String,
    val dueDate: LocalDate,
    val amountCents: Long,
)

/** A live movement of the account that a row could link to; [amountCents] is signed for the account. */
data class ExistingCandidate(
    val source: MovementSource,
    val id: String,
    val date: LocalDate,
    val amountCents: Long,
    val notes: String?,
    val kind: MovementKind,
)

/** The database's view of one statement row: what it is, what it suggests and every candidate. */
data class BankRowPreview(
    val index: Int,
    val status: BankRowStatus,
    val suggestedAction: BankRowAction,
    val commitmentId: String?,
    val dueDate: LocalDate?,
    val matchSource: MovementSource?,
    val matchId: String?,
    val envelopeId: String?,
    val pending: List<PendingCandidate>,
    val existing: List<ExistingCandidate>,
)

/**
 * The decision for one row, as `apply_bank_import` takes it. For [BankRowAction.Create]: [kind] (an expense or an
 * income follows the sign; a transfer needs [otherAccountId]), [envelopeId] (null = Unassigned) and [notes]. For
 * Confirm: [commitmentId] and [dueDate]. For Link: [linkSource] and [linkId].
 */
data class BankRowChoice(
    val ref: String,
    val date: LocalDate,
    val amountCents: Long,
    val description: String,
    val action: BankRowAction,
    val kind: EntryKind? = null,
    val envelopeId: String? = null,
    val otherAccountId: String? = null,
    val notes: String? = null,
    val commitmentId: String? = null,
    val dueDate: LocalDate? = null,
    val linkSource: MovementSource? = null,
    val linkId: String? = null,
)

/** An import with its counts by action and the dates it covers (`bank_import_overview`). */
data class BankImportSummary(
    val id: String,
    val accountId: String,
    val fileName: String?,
    val tagId: String?,
    val createdDay: LocalDate?,
    val rowsTotal: Int,
    val created: Int,
    val confirmed: Int,
    val linked: Int,
    val ignored: Int,
    val firstDate: LocalDate?,
    val lastDate: LocalDate?,
    val isVoided: Boolean,
)
