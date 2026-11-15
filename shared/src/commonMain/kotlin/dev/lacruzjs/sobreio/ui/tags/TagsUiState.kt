package dev.lacruzjs.sobreio.ui.tags

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.TagUsage
import dev.lacruzjs.sobreio.ui.theme.TagColor
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.StringResource

/** Which bottom sheet of the Tags screen is open. */
enum class TagsSheet { None, Edit, Merge }

data class TagsUiState(
    val group: Group? = null,
    /** Live tags with their movement count, ordered by name. */
    val tags: List<TagUsage> = emptyList(),
    val archivedTags: List<TagUsage> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val sheet: TagsSheet = TagsSheet.None,
    /** The tag the Edit sheet changes; null when it creates one. */
    val editingId: String? = null,
    val name: String = "",
    val nameError: StringResource? = null,
    val color: TagColor = TagColor.Default,
    /** The tag the Merge sheet folds into another one. */
    val mergeSource: TagUsage? = null,
    val mergeTargetId: String? = null,
    val mergeTargetError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of saving or merging, shown inside the open sheet. */
    val submitError: AppError? = null,
    /** Failure of an action outside the sheets (delete, reopen, undo). */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
) {
    /** Any member of a live group manages tags; archived groups and groups pending deletion are read only. */
    val canEdit: Boolean get() = group?.let { !it.isArchived && !it.isPendingDeletion } == true

    /** Merging needs another live tag to merge into. */
    val canMerge: Boolean get() = tags.size > 1

    /** The live tags the source can be merged into. */
    val mergeCandidates: List<TagUsage> get() = tags.filter { it.id != mergeSource?.id }
}
