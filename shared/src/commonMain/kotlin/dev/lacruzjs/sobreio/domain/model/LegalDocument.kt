package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** The two documents a user accepts (D-57). */
enum class LegalDocumentKind { Terms, Privacy }

/**
 * A legal document in force: its version (the day it took effect) and where to read it. The text lives on the
 * website; the database holds the links, so the app has no URL of its own.
 */
data class LegalDocument(
    val kind: LegalDocumentKind,
    val version: LocalDate,
    val urlEs: String,
    val urlEn: String,
) {
    /** The link in the app's language: Spanish for any Spanish locale, English for everything else. */
    fun urlFor(languageTag: String): String = if (languageTag.lowercase().startsWith("es")) urlEs else urlEn
}

/** A document in force and what the signed-in user did with it (`legal_status`). */
data class LegalDocumentStatus(
    val document: LegalDocument,
    val acceptedVersion: LocalDate?,
    val acceptedAt: Instant?,
    val needsAcceptance: Boolean,
)
