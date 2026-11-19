package dev.lacruzjs.sobreio.fake

import kotlinx.datetime.LocalDate
import dev.lacruzjs.sobreio.data.repository.EnvelopeDetailRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage
import dev.lacruzjs.sobreio.domain.model.FundingCommitment

/**
 * Serves the lists it is given in pages of the requested size. Calls are recorded in [calls]; set the `*Result` fields to
 * script failures.
 */
class FakeEnvelopeDetailRepository(var envelopes: List<Envelope> = emptyList()) : EnvelopeDetailRepository {

    val calls = mutableListOf<String>()

    var periods: List<EnvelopePeriod> = emptyList()
    var outlook: EnvelopeOutlook? = null
    var transfers: List<EnvelopeTransfer> = emptyList()
    var fundingCommitments: List<FundingCommitment> = emptyList()

    var periodsResult: Result<List<EnvelopePeriod>>? = null
    var outlookResult: Result<EnvelopeOutlook?>? = null
    var transfersResult: Result<EnvelopeTransferPage>? = null

    override suspend fun getEnvelope(envelopeId: String): Result<Envelope> {
        calls += "getEnvelope($envelopeId)"
        return envelopes.firstOrNull { it.id == envelopeId }?.let { Result.success(it) } ?: Result.failure(AppError.NotFound)
    }

    override suspend fun getPeriods(envelopeId: String, limit: Int): Result<List<EnvelopePeriod>> {
        calls += "getPeriods($envelopeId, $limit)"
        return periodsResult ?: Result.success(periods.takeLast(limit))
    }

    var balanceAt: Long = 0
    var outlookUntil: EnvelopeOutlook? = null

    override suspend fun getBalanceAt(envelopeId: String, day: LocalDate): Result<Long> {
        calls += "getBalanceAt($envelopeId, $day)"
        return Result.success(balanceAt)
    }

    override suspend fun getOutlookUntil(groupId: String, envelopeId: String, until: LocalDate): Result<EnvelopeOutlook?> {
        calls += "getOutlookUntil($envelopeId, $until)"
        return Result.success(outlookUntil)
    }

    override suspend fun getOutlook(groupId: String, envelopeId: String): Result<EnvelopeOutlook?> {
        calls += "getOutlook($groupId, $envelopeId)"
        return outlookResult ?: Result.success(outlook)
    }

    override suspend fun getTransfers(envelopeId: String, offset: Int, limit: Int): Result<EnvelopeTransferPage> {
        calls += "getTransfers($envelopeId, $offset, $limit)"
        return transfersResult ?: Result.success(EnvelopeTransferPage(transfers.drop(offset).take(limit), transfers.size))
    }

    override suspend fun getFundingCommitments(envelopeId: String): Result<List<FundingCommitment>> {
        calls += "getFundingCommitments($envelopeId)"
        return Result.success(fundingCommitments)
    }
}
