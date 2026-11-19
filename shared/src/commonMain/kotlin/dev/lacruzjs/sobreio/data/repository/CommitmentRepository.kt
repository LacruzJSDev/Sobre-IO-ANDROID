package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.PendingMatch
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentChange
import dev.lacruzjs.sobreio.domain.model.CommitmentDraft
import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.OccurrencePage
import dev.lacruzjs.sobreio.domain.model.LinkedCommitmentDraft
import kotlinx.datetime.LocalDate

/**
 * The commitments of a group: dated payments, incomes and transfers, and the contributions to another group. Reads need
 * membership; writes need owner or admin (FZ016 otherwise). Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface CommitmentRepository {

    /**
     * Every commitment (live and archived) that has a version touching the account, as its source or as the destination
     * of a transfer, each with ALL its versions newest first. The caller decides which version is "current".
     */
    suspend fun getAccountCommitments(accountId: String): Result<List<Commitment>>

    /** One commitment with its versions; fails with [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when it is not visible. */
    suspend fun getCommitment(id: String): Result<Commitment>

    /**
     * `commitment_funding_status`: the reservation of a funded commitment in its current cycle, or null when the database
     * returns nothing (not funded in force, archived, no due date to prepare).
     */
    suspend fun getFundingStatus(commitmentId: String): Result<CommitmentFunding?>

    /**
     * A page of the resolved due items (registered or skipped) of a commitment, newest due date first: at most [limit]
     * from [offset], each with the movement it registered, plus the exact total the commitment has.
     */
    suspend fun getOccurrences(commitmentId: String, offset: Int = 0, limit: Int = 20): Result<OccurrencePage>

    /**
     * The ids among [commitmentIds] that have a due date inside [from]..[to] (both included) already registered as a real
     * movement (a skipped one does not count). Read from the resolved due items; nothing is computed.
     */
    suspend fun getPaidCommitmentIds(commitmentIds: List<String>, from: LocalDate, to: LocalDate): Result<Set<String>>

    /**
     * The links of the contributions among [commitmentIds] (either half). A link is only visible to members of both
     * groups, so a contribution without a row here cannot be changed from this group.
     */
    suspend fun getLinks(commitmentIds: List<String>): Result<List<CommitmentLink>>

    /**
     * The pending due dates of [accountId] that look like a movement of [signedCents] on [date] (`match_pending`:
     * same direction, ±5 days, amount within 20 %), the closest first. It only suggests: registering is `confirm_occurrence`.
     */
    suspend fun matchPending(accountId: String, signedCents: Long, date: LocalDate): Result<List<PendingMatch>>

    /** `create_commitment` (not for contributions); returns the commitment id. A taken name fails with DuplicateName. */
    suspend fun createCommitment(draft: CommitmentDraft): Result<String>

    /** `change_commitment`: closes the version in force and opens a new one from [CommitmentChange.effectiveFrom] (FZ018 if impossible). */
    suspend fun changeCommitment(id: String, change: CommitmentChange): Result<Unit>

    /** Changes the name only. */
    suspend fun renameCommitment(id: String, name: String): Result<Unit>

    /** Archives it at once (`archived_at`). */
    suspend fun archiveCommitment(id: String): Result<Unit>

    /** Brings an archived commitment back; a name taken meanwhile fails with DuplicateName. */
    suspend fun unarchiveCommitment(id: String): Result<Unit>

    /** `create_linked_commitment`: a contribution to another group (the caller is owner or admin of both); returns the link id. */
    suspend fun createLinkedCommitment(draft: LinkedCommitmentDraft): Result<String>

    /** `change_linked_commitment`: the amount of both halves from [effectiveFrom] (today when null). */
    suspend fun changeLinkedCommitment(linkId: String, amountCents: Long, effectiveFrom: LocalDate?): Result<Unit>

    /** `archive_linked_commitment`: archives both halves. */
    suspend fun archiveLinkedCommitment(linkId: String): Result<Unit>

    /** Brings both halves back with one update (they are only valid archived or live together). */
    suspend fun unarchiveLinkedCommitment(link: CommitmentLink): Result<Unit>
}
