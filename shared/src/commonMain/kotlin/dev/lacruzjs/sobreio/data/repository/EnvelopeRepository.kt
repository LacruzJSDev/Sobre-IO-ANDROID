package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary

/** The envelopes of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface EnvelopeRepository {

    /** Makes the database assign the monthly targets of the current period. Callers may ignore a failure. */
    suspend fun materializeThroughToday(groupId: String): Result<Unit>

    /**
     * Every envelope of the group, live and archived (see [Envelope.isArchived]), ordered by name, with the balance the
     * database computed and the colour and icon read from the `envelopes` table. Includes the system "Unassigned"
     * envelope; the `external` one is never returned.
     */
    suspend fun getEnvelopes(groupId: String): Result<List<Envelope>>

    /** Total inside the envelopes and balance of "Unassigned", as the database computes them. */
    suspend fun getSummary(groupId: String): Result<EnvelopeSummary>

    /**
     * Creates a spending or saving envelope with every field and returns its id. Owner or admin only. Fails with
     * [dev.lacruzjs.sobreio.domain.error.AppError.DuplicateName] when the name is taken.
     */
    suspend fun createEnvelope(groupId: String, draft: EnvelopeDraft): Result<String>

    /** Owner or admin only; anyone else fails with a business rule error (FZ016). */
    suspend fun updateEnvelope(id: String, draft: EnvelopeDraft): Result<Unit>

    /** Archives the envelope. Fails with FZ008 unless the balance is zero and it has no live commitments. */
    suspend fun archiveEnvelope(id: String): Result<Unit>

    /** Brings an archived envelope back. Fails with a duplicate name when a live envelope took its name. */
    suspend fun unarchiveEnvelope(id: String): Result<Unit>

    /** Moves [amountCents] (> 0) from one envelope to another. Any member may do it. */
    suspend fun assign(groupId: String, fromEnvelopeId: String, toEnvelopeId: String, amountCents: Long): Result<Unit>
}
