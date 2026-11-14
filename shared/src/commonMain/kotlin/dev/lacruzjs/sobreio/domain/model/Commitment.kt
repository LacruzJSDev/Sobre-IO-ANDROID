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
}

/** The link between the two halves of a contribution to another group (`commitment_links`). */
data class CommitmentLink(
    val id: String,
    val outGroupId: String,
    val inGroupId: String,
    val outCommitmentId: String,
    val inCommitmentId: String,
)

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
