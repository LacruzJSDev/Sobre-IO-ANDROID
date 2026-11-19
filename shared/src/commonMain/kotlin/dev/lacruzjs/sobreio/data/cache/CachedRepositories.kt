package dev.lacruzjs.sobreio.data.cache

import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import dev.lacruzjs.sobreio.data.repository.BankImportRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeDetailRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.data.repository.MovementFormRepository
import dev.lacruzjs.sobreio.data.repository.MovementRepository
import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.datetime.LocalDate

/*
 * Every repository the screens read goes through [QueryCache]: each read here is the remote read behind a cache key
 * (its name and arguments). Writes are not overridden: they go straight to the remote repository through delegation
 * (`by remote`) and the HTTP client invalidates the cache when they reach Supabase ([cacheInvalidationPlugin]), so no
 * write can forget to. Reads that depend on what the user types (compare with bank, bank import preview) are not cached.
 */

class CachedGroupRepository(private val remote: GroupRepository, private val cache: QueryCache) : GroupRepository by remote {
    override suspend fun getGroups() = cache.get("groups") { remote.getGroups() }
    override suspend fun getGroup(id: String) = cache.get("group", id) { remote.getGroup(id) }
    override suspend fun getInvitations(groupId: String) = cache.get("invitations", groupId) { remote.getInvitations(groupId) }
}

class CachedMemberRepository(private val remote: MemberRepository, private val cache: QueryCache) : MemberRepository by remote {
    override suspend fun getMembers(groupId: String) = cache.get("members", groupId) { remote.getMembers(groupId) }
}

class CachedAccountRepository(private val remote: AccountRepository, private val cache: QueryCache) : AccountRepository by remote {
    override suspend fun getAccounts(groupId: String) = cache.get("accounts", groupId) { remote.getAccounts(groupId) }
    override suspend fun getSummary(groupId: String) = cache.get("accounts.summary", groupId) { remote.getSummary(groupId) }
    override suspend fun getBalanceSeries(groupId: String, from: LocalDate, to: LocalDate, points: Int) =
        cache.get("group.balanceSeries", groupId, from, to, points) { remote.getBalanceSeries(groupId, from, to, points) }
}

class CachedAccountDetailRepository(
    private val remote: AccountDetailRepository,
    private val cache: QueryCache,
) : AccountDetailRepository by remote {
    override suspend fun getForecast(groupId: String, accountId: String) =
        cache.get("account.forecast", groupId, accountId) { remote.getForecast(groupId, accountId) }
    override suspend fun getBalanceAt(accountId: String, day: LocalDate) =
        cache.get("account.balanceAt", accountId, day) { remote.getBalanceAt(accountId, day) }
    override suspend fun getForecasts(groupId: String) = cache.get("account.forecasts", groupId) { remote.getForecasts(groupId) }
    override suspend fun getBalanceSeries(accountId: String, from: LocalDate, to: LocalDate, points: Int) =
        cache.get("account.balanceSeries", accountId, from, to, points) { remote.getBalanceSeries(accountId, from, to, points) }
    override suspend fun getProjection(accountId: String) = cache.get("account.projection", accountId) { remote.getProjection(accountId) }
    override suspend fun getPeriodFlows(accountId: String, periods: Int) =
        cache.get("account.periodFlows", accountId, periods) { remote.getPeriodFlows(accountId, periods) }
    override suspend fun getUpcoming(groupId: String, accountId: String, from: LocalDate, to: LocalDate) =
        cache.get("account.upcoming", groupId, accountId, from, to) { remote.getUpcoming(groupId, accountId, from, to) }
    override suspend fun getLatestMovements(accountId: String, count: Int) =
        cache.get("account.latestMovements", accountId, count) { remote.getLatestMovements(accountId, count) }
}

class CachedEnvelopeRepository(private val remote: EnvelopeRepository, private val cache: QueryCache) : EnvelopeRepository by remote {
    /**
     * Writes only what the calendar implies, so it is safe to run once per 30 s per group (see [READ_ONLY_RPCS]). A
     * cache-only pass skips it: it has nothing to show, and painting the cached figures must not wait for it.
     */
    override suspend fun materializeThroughToday(groupId: String): Result<Unit> =
        if (currentCoroutineContext()[CacheOnlyPass] != null) Result.success(Unit)
        else cache.get("materialize", groupId) { remote.materializeThroughToday(groupId) }
    override suspend fun getEnvelopes(groupId: String) = cache.get("envelopes", groupId) { remote.getEnvelopes(groupId) }
    override suspend fun getSummary(groupId: String) = cache.get("envelopes.summary", groupId) { remote.getSummary(groupId) }
    override suspend fun getProgress(groupId: String) = cache.get("envelopes.progress", groupId) { remote.getProgress(groupId) }
}

class CachedEnvelopeDetailRepository(
    private val remote: EnvelopeDetailRepository,
    private val cache: QueryCache,
) : EnvelopeDetailRepository by remote {
    override suspend fun getEnvelope(envelopeId: String) = cache.get("envelope", envelopeId) { remote.getEnvelope(envelopeId) }
    override suspend fun getBalanceAt(envelopeId: String, day: LocalDate) =
        cache.get("envelope.balanceAt", envelopeId, day) { remote.getBalanceAt(envelopeId, day) }
    override suspend fun getOutlookUntil(groupId: String, envelopeId: String, until: LocalDate) =
        cache.get("envelope.outlookUntil", groupId, envelopeId, until) { remote.getOutlookUntil(groupId, envelopeId, until) }
    override suspend fun getPeriods(envelopeId: String, limit: Int) =
        cache.get("envelope.periods", envelopeId, limit) { remote.getPeriods(envelopeId, limit) }
    override suspend fun getOutlook(groupId: String, envelopeId: String) =
        cache.get("envelope.outlook", groupId, envelopeId) { remote.getOutlook(groupId, envelopeId) }
    override suspend fun getTransfers(envelopeId: String, offset: Int, limit: Int) =
        cache.get("envelope.transfers", envelopeId, offset, limit) { remote.getTransfers(envelopeId, offset, limit) }
    override suspend fun getFundingCommitments(envelopeId: String) =
        cache.get("envelope.fundingCommitments", envelopeId) { remote.getFundingCommitments(envelopeId) }
}

class CachedEnvelopeTransferRepository(
    private val remote: EnvelopeTransferRepository,
    private val cache: QueryCache,
) : EnvelopeTransferRepository by remote {
    override suspend fun getTransfers(groupId: String, envelopeId: String?, offset: Int, limit: Int) =
        cache.get("envelopeTransfers", groupId, envelopeId, offset, limit) { remote.getTransfers(groupId, envelopeId, offset, limit) }
    override suspend fun getTransfer(id: String) = cache.get("envelopeTransfer", id) { remote.getTransfer(id) }
}

class CachedGroupSummaryRepository(
    private val remote: GroupSummaryRepository,
    private val cache: QueryCache,
) : GroupSummaryRepository by remote {
    override suspend fun getOutlook(groupId: String) = cache.get("group.outlook", groupId) { remote.getOutlook(groupId) }
    override suspend fun getOutlookUntil(groupId: String, until: LocalDate) =
        cache.get("group.outlookUntil", groupId, until) { remote.getOutlookUntil(groupId, until) }
    override suspend fun getPending(groupId: String) = cache.get("group.pending", groupId) { remote.getPending(groupId) }
    override suspend fun getUpcoming(groupId: String, from: LocalDate, to: LocalDate) =
        cache.get("group.upcoming", groupId, from, to) { remote.getUpcoming(groupId, from, to) }
    override suspend fun getShortfalls(groupId: String) = cache.get("group.shortfalls", groupId) { remote.getShortfalls(groupId) }
    override suspend fun getFreeProjection(groupId: String) = cache.get("group.freeProjection", groupId) { remote.getFreeProjection(groupId) }
    override suspend fun getTransferPlan(groupId: String) = cache.get("group.transferPlan", groupId) { remote.getTransferPlan(groupId) }
    override suspend fun getPeriodFlows(groupId: String, periods: Int) =
        cache.get("group.periodFlows", groupId, periods) { remote.getPeriodFlows(groupId, periods) }
    override suspend fun getEnvelopeSpending(groupId: String, periodStart: LocalDate) =
        cache.get("group.envelopeSpending", groupId, periodStart) { remote.getEnvelopeSpending(groupId, periodStart) }
}

class CachedCommitmentRepository(private val remote: CommitmentRepository, private val cache: QueryCache) : CommitmentRepository by remote {
    override suspend fun getAccountCommitments(accountId: String) =
        cache.get("account.commitments", accountId) { remote.getAccountCommitments(accountId) }
    override suspend fun getCommitment(id: String) = cache.get("commitment", id) { remote.getCommitment(id) }
    override suspend fun getFundingStatus(commitmentId: String) =
        cache.get("commitment.funding", commitmentId) { remote.getFundingStatus(commitmentId) }
    override suspend fun getOccurrences(commitmentId: String, offset: Int, limit: Int) =
        cache.get("commitment.occurrences", commitmentId, offset, limit) { remote.getOccurrences(commitmentId, offset, limit) }
    override suspend fun getPaidCommitmentIds(commitmentIds: List<String>, from: LocalDate, to: LocalDate) =
        cache.get("commitments.paid", commitmentIds, from, to) { remote.getPaidCommitmentIds(commitmentIds, from, to) }
    override suspend fun matchPending(accountId: String, signedCents: Long, date: LocalDate) =
        cache.get("commitments.matchPending", accountId, signedCents, date) { remote.matchPending(accountId, signedCents, date) }
    override suspend fun getLinks(commitmentIds: List<String>) = cache.get("commitments.links", commitmentIds) { remote.getLinks(commitmentIds) }
}

class CachedMovementRepository(private val remote: MovementRepository, private val cache: QueryCache) : MovementRepository by remote {
    override suspend fun search(groupId: String, filter: MovementFilter, limit: Int, offset: Int, withTotal: Boolean) =
        cache.get("movements", groupId, filter, limit, offset, withTotal) { remote.search(groupId, filter, limit, offset, withTotal) }
    override suspend fun getTotals(groupId: String, filter: MovementFilter) =
        cache.get("movements.totals", groupId, filter) { remote.getTotals(groupId, filter) }
    override suspend fun getReport(groupId: String, filter: MovementFilter) =
        cache.get("movements.report", groupId, filter) { remote.getReport(groupId, filter) }
}

class CachedMovementFormRepository(
    private val remote: MovementFormRepository,
    private val cache: QueryCache,
) : MovementFormRepository by remote {
    override suspend fun getTransaction(id: String) = cache.get("transaction", id) { remote.getTransaction(id) }
    override suspend fun getTransfer(id: String) = cache.get("accountTransfer", id) { remote.getTransfer(id) }
    override suspend fun getTagIds(transactionId: String) = cache.get("transaction.tags", transactionId) { remote.getTagIds(transactionId) }
}

class CachedTagRepository(private val remote: TagRepository, private val cache: QueryCache) : TagRepository by remote {
    override suspend fun getTags(groupId: String) = cache.get("tags", groupId) { remote.getTags(groupId) }
    override suspend fun getUsage(groupId: String) = cache.get("tags.usage", groupId) { remote.getUsage(groupId) }
}

class CachedBankImportRepository(private val remote: BankImportRepository, private val cache: QueryCache) : BankImportRepository by remote {
    override suspend fun getLastImport(accountId: String) = cache.get("bank.lastImport", accountId) { remote.getLastImport(accountId) }
}

class CachedCalendarRepository(private val remote: CalendarRepository, private val cache: QueryCache) : CalendarRepository by remote {
    override suspend fun getToday(groupId: String) = cache.get("today", groupId) { remote.getToday(groupId) }
    override suspend fun getCurrentPeriod(groupId: String) = cache.get("currentPeriod", groupId) { remote.getCurrentPeriod(groupId) }
    override suspend fun getDateContext(groupId: String) = cache.get("dateContext", groupId) { remote.getDateContext(groupId) }
    override suspend fun getPeriods(groupId: String, limit: Int) = cache.get("periods", groupId, limit) { remote.getPeriods(groupId, limit) }
}
