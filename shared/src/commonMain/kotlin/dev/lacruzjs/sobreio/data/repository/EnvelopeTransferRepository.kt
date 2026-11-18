package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage

/**
 * The moves between envelopes of a group (`envelope_transfers`), for the screen that lists, edits and voids them all.
 * Any member may write them (row level security); the database guards the automatic ones (D-15, D-16). Failures are
 * [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface EnvelopeTransferRepository {

    /**
     * The live moves of the group, or only those from or to [envelopeId] when it is given, newest first, [limit] rows from
     * [offset], with the exact total (`count(exact)`). Moves of commitment origin carry the commitment's name.
     */
    suspend fun getTransfers(groupId: String, envelopeId: String?, offset: Int, limit: Int): Result<EnvelopeTransferPage>

    /** One live move; [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when it is voided or not visible. */
    suspend fun getTransfer(id: String): Result<EnvelopeTransfer>

    /** A manual move in the current period (the database sets the period). */
    suspend fun createTransfer(groupId: String, draft: EnvelopeTransferDraft): Result<Unit>

    /** A plain update of a live move; the database keeps the origin and rejects envelopes an automatic one cannot have. */
    suspend fun updateTransfer(id: String, draft: EnvelopeTransferDraft): Result<Unit>

    /** Voids a move (`voided_at`), only of the origins the app voids. FZ016 when RLS hides it. */
    suspend fun voidTransfer(id: String): Result<Unit>
}
