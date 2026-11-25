package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.LegalDocument
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus
import kotlinx.datetime.LocalDate

/**
 * The terms of use and the privacy policy (D-57). Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. A
 * database without D-57 answers as a failure too; callers treat it as "nothing to accept".
 */
interface LegalRepository {

    /** The documents in force with their links; works without a session (sign-up screen). Empty when none is published. */
    suspend fun getDocumentsInForce(): Result<List<LegalDocument>>

    /** For the signed-in user: each document in force and whether they still have to accept it. */
    suspend fun getStatus(): Result<List<LegalDocumentStatus>>

    /** Records that the signed-in user accepts these versions; fails with FZ025 when one is no longer in force. */
    suspend fun accept(termsVersion: LocalDate, privacyVersion: LocalDate): Result<Unit>
}
