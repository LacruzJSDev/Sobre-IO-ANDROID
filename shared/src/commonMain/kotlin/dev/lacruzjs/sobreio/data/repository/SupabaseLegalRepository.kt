package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.model.LegalDocument
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseLegalRepository(client: SupabaseClient) : LegalRepository {

    private val postgrest = client.postgrest

    override suspend fun getDocumentsInForce(): Result<List<LegalDocument>> = safeCall {
        postgrest.rpc("legal_documents_in_force").decodeList<LegalDocumentDto>().mapNotNull { it.toDomain() }
    }

    override suspend fun getStatus(): Result<List<LegalDocumentStatus>> = safeCall {
        postgrest.rpc("legal_status").decodeList<LegalStatusDto>().mapNotNull { dto ->
            val document = LegalDocumentDto(dto.document, dto.version, dto.urlEs, dto.urlEn).toDomain()
                ?: return@mapNotNull null
            LegalDocumentStatus(
                document = document,
                acceptedVersion = dto.acceptedVersion?.let(LocalDate::parse),
                acceptedAt = dto.acceptedAt?.let(Instant::parse),
                needsAcceptance = dto.needsAcceptance,
            )
        }
    }

    override suspend fun accept(termsVersion: LocalDate, privacyVersion: LocalDate): Result<Unit> = safeCall {
        postgrest.rpc(
            "accept_legal",
            buildJsonObject {
                put("p_terms_version", termsVersion.toString())
                put("p_privacy_version", privacyVersion.toString())
            },
        )
        Unit
    }

    @Serializable
    private data class LegalDocumentDto(
        val document: String,
        val version: String,
        @SerialName("url_es") val urlEs: String,
        @SerialName("url_en") val urlEn: String,
    ) {
        /** Null for a kind this version of the app does not know, so a new document never breaks it. */
        fun toDomain(): LegalDocument? {
            val kind = when (document) {
                "terms" -> LegalDocumentKind.Terms
                "privacy" -> LegalDocumentKind.Privacy
                else -> return null
            }
            return LegalDocument(kind, LocalDate.parse(version), urlEs, urlEn)
        }
    }

    @Serializable
    private data class LegalStatusDto(
        val document: String,
        val version: String,
        @SerialName("url_es") val urlEs: String,
        @SerialName("url_en") val urlEn: String,
        @SerialName("accepted_version") val acceptedVersion: String? = null,
        @SerialName("accepted_at") val acceptedAt: String? = null,
        @SerialName("needs_acceptance") val needsAcceptance: Boolean,
    )
}
