package dev.lacruzjs.sobreio.ui.legal

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.LegalDocument
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus

/** What the root shows between the session and the app (D-57). */
sealed interface LegalGateUiState {
    /** Reading what the user accepted; the root keeps the splash. */
    data object Checking : LegalGateUiState

    /** Nothing to accept (or it could not be read): the app shows. */
    data object Clear : LegalGateUiState

    /** The user must accept the terms and the privacy policy in force before using the app. */
    data class Required(
        val terms: LegalDocument,
        val privacy: LegalDocument,
        /** They accepted an older version: the screen says the documents changed instead of welcoming them. */
        val isUpdate: Boolean,
        val accepted: Boolean = false,
        val isAccepting: Boolean = false,
        val isSigningOut: Boolean = false,
        val error: AppError? = null,
    ) : LegalGateUiState {
        val canContinue: Boolean get() = accepted && !isAccepting && !isSigningOut
    }
}

/**
 * The gate for what `legal_status` returned. The database accepts both documents in one call, so the gate only asks
 * when both are in force; with none published (or only one) there is nothing the user could accept, and the app shows.
 */
fun legalGateStateOf(statuses: List<LegalDocumentStatus>): LegalGateUiState {
    if (statuses.none { it.needsAcceptance }) return LegalGateUiState.Clear
    val terms = statuses.firstOrNull { it.document.kind == LegalDocumentKind.Terms } ?: return LegalGateUiState.Clear
    val privacy = statuses.firstOrNull { it.document.kind == LegalDocumentKind.Privacy } ?: return LegalGateUiState.Clear
    return LegalGateUiState.Required(
        terms = terms.document,
        privacy = privacy.document,
        isUpdate = statuses.any { it.acceptedVersion != null },
    )
}
