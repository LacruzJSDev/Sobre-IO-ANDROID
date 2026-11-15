package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.ConfirmedFact
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.EnvelopeSpending
import dev.lacruzjs.sobreio.domain.model.FreePoint
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import kotlinx.datetime.LocalDate

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeGroupSummaryRepository : GroupSummaryRepository {

    val calls = mutableListOf<String>()

    var outlook = GroupOutlook(
        periodStart = LocalDate(2026, 11, 1),
        periodEnd = LocalDate(2026, 11, 30),
        unassignedCents = 100_000,
        overspentCents = 0,
        freeTodayCents = 100_000,
        committedShortfallCents = 0,
        pendingOutCents = 0,
        pendingIncomeCents = 0,
        freeProjectedCents = 80_000,
    )
    var pending = emptyList<DueOccurrence>()
    var upcoming = emptyList<DueOccurrence>()
    var shortfalls = emptyList<AccountShortfall>()
    var freeProjection = emptyList<FreePoint>()
    var periodFlows = emptyList<PeriodFlow>()
    var spending = emptyList<EnvelopeSpending>()
    var transferPlan = emptyList<TransferSuggestion>()
    var transferPlanResult: Result<List<TransferSuggestion>>? = null

    var outlookResult: Result<GroupOutlook>? = null
    var projectionResult: Result<List<FreePoint>>? = null
    var flowsResult: Result<List<PeriodFlow>>? = null
    var spendingResult: Result<List<EnvelopeSpending>>? = null
    var confirmResult: Result<ConfirmedFact>? = null
    var skipResult: Result<Unit> = Result.success(Unit)
    var voidResult: Result<Unit> = Result.success(Unit)

    /** What the last confirm received. */
    var confirmedAmount: Long? = null
    var confirmedDate: LocalDate? = null
    var confirmedCover: List<CoverItem>? = null
    var upcomingRange: Pair<LocalDate, LocalDate>? = null
    var spendingPeriodStart: LocalDate? = null
    var flowPeriods: Int? = null

    /** Due dates confirmed or skipped leave [pending], as the database does. */
    override suspend fun getOutlook(groupId: String): Result<GroupOutlook> {
        calls += "getOutlook($groupId)"
        return outlookResult ?: Result.success(outlook)
    }

    override suspend fun getPending(groupId: String): Result<List<DueOccurrence>> {
        calls += "getPending($groupId)"
        return Result.success(pending)
    }

    override suspend fun getUpcoming(groupId: String, from: LocalDate, to: LocalDate): Result<List<DueOccurrence>> {
        calls += "getUpcoming($from, $to)"
        upcomingRange = from to to
        return Result.success(upcoming)
    }

    override suspend fun getShortfalls(groupId: String): Result<List<AccountShortfall>> = Result.success(shortfalls)

    override suspend fun getFreeProjection(groupId: String): Result<List<FreePoint>> {
        calls += "getFreeProjection($groupId)"
        return projectionResult ?: Result.success(freeProjection)
    }

    override suspend fun getTransferPlan(groupId: String): Result<List<TransferSuggestion>> {
        calls += "getTransferPlan($groupId)"
        return transferPlanResult ?: Result.success(transferPlan)
    }

    override suspend fun getPeriodFlows(groupId: String, periods: Int): Result<List<PeriodFlow>> {
        calls += "getPeriodFlows($groupId, $periods)"
        flowPeriods = periods
        return flowsResult ?: Result.success(periodFlows)
    }

    override suspend fun getEnvelopeSpending(groupId: String, periodStart: LocalDate): Result<List<EnvelopeSpending>> {
        calls += "getEnvelopeSpending($periodStart)"
        spendingPeriodStart = periodStart
        return spendingResult ?: Result.success(spending)
    }

    override suspend fun confirm(
        commitmentId: String,
        kind: CommitmentKind,
        dueDate: LocalDate,
        amountCents: Long,
        date: LocalDate,
        cover: List<CoverItem>,
    ): Result<ConfirmedFact> {
        calls += "confirm($commitmentId, $dueDate)"
        confirmedAmount = amountCents
        confirmedDate = date
        confirmedCover = cover
        val result = confirmResult ?: Result.success(ConfirmedFact("fact-$commitmentId", kind))
        if (result.isSuccess) pending = pending.filterNot { it.commitmentId == commitmentId && it.dueDate == dueDate }
        return result
    }

    override suspend fun skip(commitmentId: String, dueDate: LocalDate): Result<Unit> {
        calls += "skip($commitmentId, $dueDate)"
        if (skipResult.isSuccess) pending = pending.filterNot { it.commitmentId == commitmentId && it.dueDate == dueDate }
        return skipResult
    }

    override suspend fun voidConfirmed(fact: ConfirmedFact): Result<Unit> {
        calls += "void(${fact.kind}, ${fact.id})"
        return voidResult
    }
}
