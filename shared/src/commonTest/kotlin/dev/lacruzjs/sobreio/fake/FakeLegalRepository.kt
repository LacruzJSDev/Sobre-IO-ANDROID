package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.LegalRepository
import dev.lacruzjs.sobreio.domain.model.LegalDocument
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus
import kotlinx.datetime.LocalDate

/** Scripted answers; [accepted] records each accept call as "terms/privacy". */
class FakeLegalRepository(
    var inForce: Result<List<LegalDocument>> = Result.success(emptyList()),
    var status: Result<List<LegalDocumentStatus>> = Result.success(emptyList()),
    var acceptResult: Result<Unit> = Result.success(Unit),
) : LegalRepository {

    val accepted = mutableListOf<String>()

    override suspend fun getDocumentsInForce() = inForce
    override suspend fun getStatus() = status
    override suspend fun accept(termsVersion: LocalDate, privacyVersion: LocalDate): Result<Unit> {
        accepted += "$termsVersion/$privacyVersion"
        return acceptResult
    }

    companion object {
        val VERSION = LocalDate(2026, 12, 1)
        val TERMS = LegalDocument(LegalDocumentKind.Terms, VERSION, "https://example.test/terminos/", "https://example.test/en/terms/")
        val PRIVACY = LegalDocument(LegalDocumentKind.Privacy, VERSION, "https://example.test/privacidad/", "https://example.test/en/privacy/")

        fun status(document: LegalDocument, acceptedVersion: LocalDate?, needsAcceptance: Boolean) =
            LegalDocumentStatus(document, acceptedVersion, acceptedAt = null, needsAcceptance = needsAcceptance)
    }
}
