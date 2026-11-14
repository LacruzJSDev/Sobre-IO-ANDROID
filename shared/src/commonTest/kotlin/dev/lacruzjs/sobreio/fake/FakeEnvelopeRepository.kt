package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeEnvelopeRepository(var envelopes: List<Envelope> = emptyList()) : EnvelopeRepository {

    val calls = mutableListOf<String>()

    var materializeResult: Result<Unit> = Result.success(Unit)
    var envelopesResult: Result<List<Envelope>>? = null
    var summary = EnvelopeSummary(totalCents = 0, unassignedCents = 0)
    var summaryResult: Result<EnvelopeSummary>? = null
    var createResult: Result<String> = Result.success("new-id")
    var updateResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var unarchiveResult: Result<Unit> = Result.success(Unit)
    var assignResult: Result<Unit> = Result.success(Unit)

    override suspend fun materializeThroughToday(groupId: String): Result<Unit> {
        calls += "materialize($groupId)"
        return materializeResult
    }

    override suspend fun getEnvelopes(groupId: String): Result<List<Envelope>> {
        calls += "getEnvelopes($groupId)"
        return envelopesResult ?: Result.success(envelopes)
    }

    override suspend fun getSummary(groupId: String): Result<EnvelopeSummary> {
        calls += "getSummary($groupId)"
        return summaryResult ?: Result.success(summary)
    }

    override suspend fun createEnvelope(
        groupId: String,
        name: String,
        kind: EnvelopeKind,
        monthlyTargetCents: Long,
    ): Result<String> {
        calls += "createEnvelope($groupId, $name, $kind, $monthlyTargetCents)"
        return createResult
    }

    override suspend fun updateEnvelope(
        id: String,
        name: String,
        kind: EnvelopeKind,
        monthlyTargetCents: Long,
    ): Result<Unit> {
        calls += "updateEnvelope($id, $name, $kind, $monthlyTargetCents)"
        return updateResult
    }

    override suspend fun archiveEnvelope(id: String): Result<Unit> {
        calls += "archiveEnvelope($id)"
        return archiveResult
    }

    override suspend fun unarchiveEnvelope(id: String): Result<Unit> {
        calls += "unarchiveEnvelope($id)"
        return unarchiveResult
    }

    override suspend fun assign(
        groupId: String,
        fromEnvelopeId: String,
        toEnvelopeId: String,
        amountCents: Long,
    ): Result<Unit> {
        calls += "assign($groupId, $fromEnvelopeId, $toEnvelopeId, $amountCents)"
        return assignResult
    }
}
