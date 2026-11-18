package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage
import dev.lacruzjs.sobreio.domain.model.FundingCommitment

/**
 * What the envelope detail shows besides the envelope itself: its periods, its outlook, the moves between envelopes and
 * the commitments that may fund it. Every figure comes from the database; nothing is summed here. Failures are
 * [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface EnvelopeDetailRepository {

    /**
     * One envelope with its balance, colour and icon (live or archived, the system "Unassigned" included); fails with
     * [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when it is not visible.
     */
    suspend fun getEnvelope(envelopeId: String): Result<Envelope>

    /** The last [limit] periods already created for the envelope, oldest first (`envelope_periods`). */
    suspend fun getPeriods(envelopeId: String, limit: Int = 6): Result<List<EnvelopePeriod>>

    /** `envelope_outlook_current` for the envelope; null for the system envelope and archived ones, which it skips. */
    suspend fun getOutlook(groupId: String, envelopeId: String): Result<EnvelopeOutlook?>

    /**
     * The live moves between envelopes that have [envelopeId] on either side, newest first, [limit] rows from [offset],
     * with the exact total (`count(exact)`). Moves of commitment origin carry the commitment's name.
     */
    suspend fun getTransfers(envelopeId: String, offset: Int, limit: Int): Result<EnvelopeTransferPage>

    /**
     * The live commitments with a funded version that points to the envelope, one row each. Which of them reserve money
     * in the current cycle is for `CommitmentRepository.getFundingStatus` to say.
     */
    suspend fun getFundingCommitments(envelopeId: String): Result<List<FundingCommitment>>
}
