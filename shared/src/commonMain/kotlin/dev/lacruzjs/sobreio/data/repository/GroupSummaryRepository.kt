package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.ConfirmedFact
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import kotlinx.datetime.LocalDate

/**
 * What the Group tab reads and writes: the group's outlook, the due dates to review, the ones coming, the accounts that
 * would run short, and confirming or skipping a due date. Every figure comes from the database; nothing is summed here.
 * Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface GroupSummaryRepository {

    /** `group_outlook_current` of the group. */
    suspend fun getOutlook(groupId: String): Result<GroupOutlook>

    /** `pending_occurrences`: due dates up to today that nobody registered or skipped, oldest first. */
    suspend fun getPending(groupId: String): Result<List<DueOccurrence>>

    /** `upcoming_occurrences` between [from] and [to]. */
    suspend fun getUpcoming(groupId: String, from: LocalDate, to: LocalDate): Result<List<DueOccurrence>>

    /** Accounts of the group whose forecast to the end of the period goes below zero. */
    suspend fun getShortfalls(groupId: String): Result<List<AccountShortfall>>

    /**
     * `confirm_occurrence`: registers the due date with [amountCents] on [date], covering what the envelope lacks with
     * [cover]. Returns the fact it created, whose kind says in which table it lives.
     */
    suspend fun confirm(
        commitmentId: String,
        kind: CommitmentKind,
        dueDate: LocalDate,
        amountCents: Long,
        date: LocalDate,
        cover: List<CoverItem>,
    ): Result<ConfirmedFact>

    /** `skip_occurrence`. */
    suspend fun skip(commitmentId: String, dueDate: LocalDate): Result<Unit>

    /**
     * Undoes a confirmation by voiding the fact it created (`voided_at`; a contribution with `void_group_transfer`). The
     * database then frees the due date, which becomes pending again.
     */
    suspend fun voidConfirmed(fact: ConfirmedFact): Result<Unit>
}
