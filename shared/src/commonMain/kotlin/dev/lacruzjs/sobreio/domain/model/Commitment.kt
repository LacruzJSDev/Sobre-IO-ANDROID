package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/** What a commitment does with money. The database stores it as `commitment_kind_enum`. */
enum class CommitmentKind { Expense, Income, Transfer, GroupTransferOut, GroupTransferIn }

/** The unit of "every N units" (`frequency_unit_enum`). */
enum class FrequencyUnit { Day, Week, Month, Year }

/**
 * One version of a commitment: the rules in force from [effectiveFrom] up to, but not including, [effectiveTo] (null =
 * still open). Amounts are positive cents; the sign comes from [kind] and from which account looks at it.
 * [envelopeId] is the envelope of an expense (or the source or destination envelope of a contribution); null means
 * "Unassigned" where that is allowed. [anchorDay] is only set for monthly and yearly schedules.
 */
data class CommitmentVersion(
    val id: String,
    val commitmentId: String,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val kind: CommitmentKind,
    val amountCents: Long,
    val accountId: String,
    val toAccountId: String? = null,
    val envelopeId: String? = null,
    val firstDueDate: LocalDate,
    val isRecurring: Boolean = false,
    val everyN: Int? = null,
    val unit: FrequencyUnit? = null,
    val anchorDay: Int? = null,
    val endDate: LocalDate? = null,
    val funded: Boolean = false,
    val autoRegister: Boolean = false,
    val notes: String? = null,
) {
    /** In force on [day]. */
    fun isInForceOn(day: LocalDate): Boolean = effectiveFrom <= day && (effectiveTo == null || day < effectiveTo)

    /** The version moves money of [accountId], as its source or as its destination of a transfer. */
    fun touches(accountId: String): Boolean = this.accountId == accountId || toAccountId == accountId
}

/**
 * A commitment with every version it has, newest first. [archivedAt] is set for archived ones. The versions of a
 * commitment listed for an account are all its versions, even the ones that did not touch the account.
 */
data class Commitment(
    val id: String,
    val groupId: String,
    val name: String,
    val notes: String? = null,
    val archivedAt: String? = null,
    val versions: List<CommitmentVersion> = emptyList(),
) {
    val isArchived: Boolean get() = archivedAt != null

    /** The version in force on [today], or else the newest one (an archived or finished commitment). Null with no versions. */
    fun currentVersion(today: LocalDate): CommitmentVersion? =
        versions.firstOrNull { it.isInForceOn(today) } ?: versions.maxByOrNull { it.effectiveFrom }

    /** The next version, starting after [today]: what the commitment turns into. Null when nothing is scheduled. */
    fun nextVersion(today: LocalDate): CommitmentVersion? =
        versions.filter { it.effectiveFrom > today }.minByOrNull { it.effectiveFrom }

    /**
     * The version [accountId] looks at: the one in force when it touches the account, else the first future one that does
     * (a commitment that moves here later). Null when no version in force or ahead touches it.
     */
    fun versionFor(accountId: String, today: LocalDate): CommitmentVersion? {
        val current = currentVersion(today)
        if (current != null && current.touches(accountId)) return current
        return versions.filter { it.effectiveFrom > today && it.touches(accountId) }.minByOrNull { it.effectiveFrom }
    }
}

/** The link between the two halves of a contribution to another group (`commitment_links`). */
data class CommitmentLink(
    val id: String,
    val outGroupId: String,
    val inGroupId: String,
    val outCommitmentId: String,
    val inCommitmentId: String,
)

/** How a due date was resolved (`occurrence_status_enum`): registered as a movement, or skipped. */
enum class OccurrenceStatus { Materialized, Skipped }

/**
 * One resolved due item of a commitment. [amountCents] and [date] are those of the movement it registered (null when
 * skipped): the amount is signed for a transaction and positive for a transfer, whose direction [fromAccountId] gives.
 */
data class CommitmentOccurrence(
    val dueDate: LocalDate,
    val status: OccurrenceStatus,
    val amountCents: Long? = null,
    val date: LocalDate? = null,
    val fromAccountId: String? = null,
)

/** One page of resolved due items, newest first, and the exact number of them the commitment has in all. */
data class OccurrencePage(val items: List<CommitmentOccurrence>, val total: Int)

/** What the user chooses when creating a commitment; [kind] is never [CommitmentKind.GroupTransferIn]. */
data class CommitmentDraft(
    val name: String,
    val kind: CommitmentKind,
    val amountCents: Long,
    val accountId: String,
    val firstDueDate: LocalDate,
    val toAccountId: String? = null,
    val envelopeId: String? = null,
    val isRecurring: Boolean = true,
    val everyN: Int = 1,
    val unit: FrequencyUnit = FrequencyUnit.Month,
    val endDate: LocalDate? = null,
    val funded: Boolean = false,
    val autoRegister: Boolean = false,
    val notes: String? = null,
)

/** The new rules of an existing (not linked) commitment, from [effectiveFrom]. The kind cannot change. */
data class CommitmentChange(
    val amountCents: Long,
    val accountId: String,
    val firstDueDate: LocalDate,
    val toAccountId: String? = null,
    val envelopeId: String? = null,
    val isRecurring: Boolean = true,
    val everyN: Int = 1,
    val unit: FrequencyUnit = FrequencyUnit.Month,
    val endDate: LocalDate? = null,
    val funded: Boolean = false,
    val autoRegister: Boolean = false,
    val notes: String? = null,
    val effectiveFrom: LocalDate? = null,
)

/** A contribution to another group: this group's account, the other group's account and an optional envelope on each side. */
data class LinkedCommitmentDraft(
    val name: String,
    val outAccountId: String,
    val inAccountId: String,
    val amountCents: Long,
    val firstDueDate: LocalDate,
    val isRecurring: Boolean = true,
    val everyN: Int = 1,
    val unit: FrequencyUnit = FrequencyUnit.Month,
    val endDate: LocalDate? = null,
    val outEnvelopeId: String? = null,
    val inEnvelopeId: String? = null,
    val funded: Boolean = false,
    val autoRegister: Boolean = true,
)
