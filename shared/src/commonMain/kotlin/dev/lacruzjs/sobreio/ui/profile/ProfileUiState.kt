package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.LegalDocumentKind
import dev.lacruzjs.sobreio.domain.model.LegalDocumentStatus
import kotlinx.datetime.LocalDate
import dev.lacruzjs.sobreio.domain.model.User
import org.jetbrains.compose.resources.StringResource

const val MAX_DISPLAY_NAME_LENGTH = 100

data class ProfileUiState(
    val user: User? = null,
    /** What the name field holds; starts as the user's name. */
    val name: String = "",
    val nameError: StringResource? = null,
    val isSaving: Boolean = false,
    /** Failure of saving the name. */
    val saveError: AppError? = null,
    /** The last save went through; cleared as soon as the name is edited again. */
    val saved: Boolean = false,
    /** Money is shown in blue and orange instead of green and red. */
    val colorBlindSafe: Boolean = false,
    /** The terms and privacy policy in force and when the user accepted them (D-57); empty when none is published. */
    val legal: List<LegalDocumentStatus> = emptyList(),
    /** Reading "Download my data" (D-58). */
    val isDownloading: Boolean = false,
    /** The data, read and waiting for the save dialog; the Route consumes it and sends [ProfileEvent.DataFileHandled]. */
    val pendingData: DataFile? = null,
    /** How the last download ended: saved, not saved (a message), or the read failed (an error). */
    val downloadMessage: StringResource? = null,
    val downloadError: AppError? = null,
) {
    val terms: LegalDocumentStatus? get() = legal.firstOrNull { it.document.kind == LegalDocumentKind.Terms }
    val privacy: LegalDocumentStatus? get() = legal.firstOrNull { it.document.kind == LegalDocumentKind.Privacy }

    /** The name differs from the saved one, so there is something to save. */
    val isDirty: Boolean get() = name.trim() != user?.displayName.orEmpty()
}

/** The JSON of "Download my data" and the day it was read, which names the file. */
data class DataFile(val day: LocalDate, val content: String)
