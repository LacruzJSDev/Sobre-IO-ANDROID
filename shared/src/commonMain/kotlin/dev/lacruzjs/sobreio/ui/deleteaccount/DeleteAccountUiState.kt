package dev.lacruzjs.sobreio.ui.deleteaccount

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem

data class DeleteAccountUiState(
    val isLoading: Boolean = true,
    /** Null until the preview has been read. */
    val items: List<DeletionPreviewItem>? = null,
    val loadError: AppError? = null,
    /** The "I understand" switch. */
    val confirmed: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: AppError? = null,
) {
    private fun withOutcome(outcome: DeletionOutcome) = items.orEmpty().filter { it.outcome == outcome }

    val blocked: List<DeletionPreviewItem> get() = withOutcome(DeletionOutcome.Blocked)
    val deleted: List<DeletionPreviewItem> get() = withOutcome(DeletionOutcome.Delete)
    val left: List<DeletionPreviewItem> get() = withOutcome(DeletionOutcome.Leave)

    /** The button works once the preview is known, no group blocks it and the user ticked the switch. */
    val canDelete: Boolean get() = items != null && blocked.isEmpty() && confirmed && !isDeleting
}
