package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.domain.model.PendingMatch
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentChange
import dev.lacruzjs.sobreio.domain.model.CommitmentDraft
import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentOccurrence
import dev.lacruzjs.sobreio.domain.model.LinkedCommitmentDraft
import dev.lacruzjs.sobreio.domain.model.OccurrencePage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeCommitmentRepository(var commitments: List<Commitment> = emptyList()) : CommitmentRepository {

    val calls = mutableListOf<String>()
    var links: List<CommitmentLink> = emptyList()
    var occurrences: List<CommitmentOccurrence> = emptyList()
    /** Scripted result for every page; when set, pages are not cut from [occurrences]. */
    var occurrencesResult: Result<OccurrencePage>? = null
    /** Results for the next calls with an offset above 0, taken in order (the first call is never scripted). */
    /** While set, a page with an offset above 0 waits for it, to keep a load in flight. */
    var loadMoreGate: CompletableDeferred<Unit>? = null
    val loadMoreResults = ArrayDeque<Result<OccurrencePage>>()

    var funding: CommitmentFunding? = null
    var fundingResult: Result<CommitmentFunding?>? = null
    /** Status by commitment id; an id listed here wins over [funding] (null = nothing reserved). */
    var fundingById: Map<String, CommitmentFunding?> = emptyMap()

    var createdDraft: CommitmentDraft? = null
    var createdLinkedDraft: LinkedCommitmentDraft? = null
    var changedWith: CommitmentChange? = null
    var linkedAmount: Long? = null
    var linkedFrom: LocalDate? = null

    /** Ids with a registered due date inside the period asked for; [paidResult] scripts a failure. */
    var paidIds: Set<String> = emptySet()
    var paidResult: Result<Set<String>>? = null

    var listResult: Result<List<Commitment>>? = null
    var createResult: Result<String> = Result.success("new-id")
    var changeResult: Result<Unit> = Result.success(Unit)
    var renameResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var unarchiveResult: Result<Unit> = Result.success(Unit)
    var linkedChangeResult: Result<Unit> = Result.success(Unit)

    override suspend fun getAccountCommitments(accountId: String): Result<List<Commitment>> {
        calls += "getAccountCommitments($accountId)"
        return listResult ?: Result.success(commitments)
    }

    override suspend fun getCommitment(id: String): Result<Commitment> {
        calls += "getCommitment($id)"
        return commitments.firstOrNull { it.id == id }?.let { Result.success(it) } ?: Result.failure(AppError.NotFound)
    }

    override suspend fun getPaidCommitmentIds(commitmentIds: List<String>, from: LocalDate, to: LocalDate): Result<Set<String>> {
        calls += "getPaidCommitmentIds($from, $to)"
        return paidResult ?: Result.success(paidIds.intersect(commitmentIds.toSet()))
    }

    override suspend fun getFundingStatus(commitmentId: String): Result<CommitmentFunding?> {
        calls += "getFundingStatus($commitmentId)"
        if (commitmentId in fundingById) return Result.success(fundingById[commitmentId])
        return fundingResult ?: Result.success(funding)
    }

    override suspend fun getOccurrences(commitmentId: String, offset: Int, limit: Int): Result<OccurrencePage> {
        calls += "getOccurrences($commitmentId, $offset, $limit)"
        if (offset > 0) loadMoreGate?.await()
        if (offset > 0 && loadMoreResults.isNotEmpty()) return loadMoreResults.removeFirst()
        return occurrencesResult ?: Result.success(OccurrencePage(occurrences.drop(offset).take(limit), occurrences.size))
    }

    var matches: List<PendingMatch> = emptyList()

    override suspend fun matchPending(accountId: String, signedCents: Long, date: LocalDate): Result<List<PendingMatch>> {
        calls += "matchPending($accountId, $signedCents, $date)"
        return Result.success(matches)
    }

    override suspend fun getLinks(commitmentIds: List<String>): Result<List<CommitmentLink>> =
        Result.success(links.filter { it.outCommitmentId in commitmentIds || it.inCommitmentId in commitmentIds })

    override suspend fun createCommitment(draft: CommitmentDraft): Result<String> {
        calls += "createCommitment(${draft.name}, ${draft.kind})"
        createdDraft = draft
        return createResult
    }

    override suspend fun changeCommitment(id: String, change: CommitmentChange): Result<Unit> {
        calls += "changeCommitment($id)"
        changedWith = change
        return changeResult
    }

    override suspend fun renameCommitment(id: String, name: String): Result<Unit> {
        calls += "renameCommitment($id, $name)"
        return renameResult
    }

    override suspend fun archiveCommitment(id: String): Result<Unit> {
        calls += "archiveCommitment($id)"
        return archiveResult
    }

    override suspend fun unarchiveCommitment(id: String): Result<Unit> {
        calls += "unarchiveCommitment($id)"
        return unarchiveResult
    }

    override suspend fun createLinkedCommitment(draft: LinkedCommitmentDraft): Result<String> {
        calls += "createLinkedCommitment(${draft.name})"
        createdLinkedDraft = draft
        return createResult
    }

    override suspend fun changeLinkedCommitment(linkId: String, amountCents: Long, effectiveFrom: LocalDate?): Result<Unit> {
        calls += "changeLinkedCommitment($linkId, $amountCents)"
        linkedAmount = amountCents
        linkedFrom = effectiveFrom
        return linkedChangeResult
    }

    override suspend fun archiveLinkedCommitment(linkId: String): Result<Unit> {
        calls += "archiveLinkedCommitment($linkId)"
        return archiveResult
    }

    override suspend fun unarchiveLinkedCommitment(link: CommitmentLink): Result<Unit> {
        calls += "unarchiveLinkedCommitment(${link.id})"
        return unarchiveResult
    }
}
