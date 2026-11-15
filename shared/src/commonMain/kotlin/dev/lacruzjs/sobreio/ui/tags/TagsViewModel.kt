package dev.lacruzjs.sobreio.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.MAX_TAG_NAME_LENGTH
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.TagUsage
import dev.lacruzjs.sobreio.domain.normalizeTagName
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_duplicate_name
import dev.lacruzjs.sobreio.generated.resources.tag_deleted
import dev.lacruzjs.sobreio.generated.resources.tag_reopened
import dev.lacruzjs.sobreio.generated.resources.tags_merged
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.generated.resources.validation_tag_merge_target
import dev.lacruzjs.sobreio.generated.resources.validation_tag_name_too_long
import dev.lacruzjs.sobreio.ui.theme.TagColor
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The tags of one group, with how many movements carry each (`tag_usage`). Any member of a live group creates, edits,
 * merges, deletes (archives, with Undo) and reopens tags; archived groups are read only. The database decides what is a
 * duplicate name (ignoring case, accents and extra spaces) and counts the movements.
 */
class TagsViewModel(
    private val groupId: String,
    private val tags: TagRepository,
    private val groups: GroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TagsUiState())
    val state: StateFlow<TagsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    init {
        load()
    }

    fun onEvent(event: TagsEvent) {
        when (event) {
            TagsEvent.Retry -> load()
            TagsEvent.AddClicked -> openEditSheet(null)
            is TagsEvent.EditClicked -> openEditSheet(event.tagId)
            is TagsEvent.NameChanged -> _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is TagsEvent.ColorSelected -> _state.update { it.copy(color = event.color, submitError = null) }
            TagsEvent.SaveClicked -> save()
            is TagsEvent.MergeClicked -> openMergeSheet(event.tagId)
            is TagsEvent.MergeTargetSelected ->
                _state.update { it.copy(mergeTargetId = event.tagId, mergeTargetError = null, submitError = null) }
            TagsEvent.MergeConfirmed -> merge()
            is TagsEvent.DeleteClicked -> delete(event.tagId)
            is TagsEvent.ReopenClicked -> reopen(event.tagId)
            TagsEvent.SheetDismissed -> _state.update { if (it.isSubmitting) it else it.copy(sheet = TagsSheet.None) }
            TagsEvent.UndoClicked -> undo.undo()
            TagsEvent.UndoExpired -> undo.commit()
            // Handled by the Route, which owns the back stack
            TagsEvent.BackClicked -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a delete still waiting for its snackbar has to reach the database
        undo.commitDetached()
    }

    private fun load() {
        _state.update { it.copy(isLoading = it.group == null, error = null) }
        viewModelScope.launch {
            val group = groups.getGroup(groupId)
            val usage = tags.getUsage(groupId)
            val failure = group.exceptionOrNull() ?: usage.exceptionOrNull()
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
                return@launch
            }
            val (archived, live) = usage.getOrThrow().partition { it.isArchived }
            _state.update {
                it.copy(group = group.getOrThrow(), tags = live.sortedByName(), archivedTags = archived.sortedByName(), isLoading = false)
            }
        }
    }

    private fun List<TagUsage>.sortedByName() = sortedBy { it.name.lowercase() }

    // Create and edit ----------------------------------------------------------------------------------------------

    private fun openEditSheet(tagId: String?) {
        val current = _state.value
        if (!current.canEdit) return
        val tag = tagId?.let { id -> current.tags.firstOrNull { it.id == id } }
        if (tagId != null && tag == null) return
        _state.update {
            it.copy(
                sheet = TagsSheet.Edit,
                editingId = tag?.id,
                name = tag?.name.orEmpty(),
                color = TagColor.fromHex(tag?.color) ?: TagColor.Default,
                nameError = null,
                submitError = null,
            )
        }
    }

    private fun save() {
        val s = _state.value
        if (s.isSubmitting || s.sheet != TagsSheet.Edit) return
        val name = normalizeTagName(s.name)
        val error = when {
            name.isEmpty() -> Res.string.validation_required
            name.length > MAX_TAG_NAME_LENGTH -> Res.string.validation_tag_name_too_long
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(nameError = error) }
            return
        }
        _state.update { it.copy(isSubmitting = true, nameError = null, submitError = null) }
        viewModelScope.launch {
            val result = s.editingId
                ?.let { id -> tags.updateTag(id, name, s.color.hex) }
                ?: tags.createTag(groupId, name, s.color.hex).map { }
            result
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false, sheet = TagsSheet.None, editingId = null) }
                    load()
                }
                .onFailure { e ->
                    val failure = e.asAppError()
                    _state.update {
                        if (failure == AppError.DuplicateName) {
                            it.copy(isSubmitting = false, nameError = Res.string.error_duplicate_name)
                        } else {
                            it.copy(isSubmitting = false, submitError = failure)
                        }
                    }
                }
        }
    }

    // Merge --------------------------------------------------------------------------------------------------------

    private fun openMergeSheet(tagId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val source = current.tags.firstOrNull { it.id == tagId } ?: return
        if (current.tags.size < 2) return
        _state.update {
            it.copy(sheet = TagsSheet.Merge, mergeSource = source, mergeTargetId = null, mergeTargetError = null, submitError = null)
        }
    }

    private fun merge() {
        val s = _state.value
        if (s.isSubmitting || s.sheet != TagsSheet.Merge) return
        val source = s.mergeSource ?: return
        val targetId = s.mergeTargetId
        if (targetId == null) {
            _state.update { it.copy(mergeTargetError = Res.string.validation_tag_merge_target) }
            return
        }
        // A destructive action still waiting for its snackbar is final before the merge
        undo.commit()
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            tags.mergeTags(source.id, targetId)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false, sheet = TagsSheet.None, mergeSource = null) }
                    // Merging cannot be undone: a plain notice, no "Undo"
                    undo.notify(Res.string.tags_merged, source.name)
                    load()
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    // Delete and reopen --------------------------------------------------------------------------------------------

    /**
     * No confirmation: the tag leaves the list at once and is archived right away; its movements keep it. The snackbar's
     * "Undo" unarchives it.
     */
    private fun delete(tagId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val tag = current.tags.firstOrNull { it.id == tagId } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        val archived = tag.copy(archivedAt = Clock.System.now().toString())
        _state.update {
            it.copy(
                tags = it.tags - tag,
                archivedTags = (it.archivedTags + archived).sortedByName(),
                actionError = null,
            )
        }
        val restore = {
            _state.update { it.copy(tags = (it.tags + tag).sortedByName(), archivedTags = it.archivedTags - archived) }
            load()
        }
        viewModelScope.launch {
            tags.archiveTag(tag.id)
                .onSuccess {
                    undo.show(
                        UndoableAction(
                            message = Res.string.tag_deleted,
                            argument = tag.name,
                            commit = { Result.success(Unit) },
                            undo = { tags.unarchiveTag(tag.id) },
                            restore = restore,
                        ),
                    )
                }
                .onFailure { e ->
                    restore()
                    _state.update { it.copy(actionError = e.asAppError()) }
                }
        }
    }

    /** An archived tag becomes live again, unless a live one took its name meanwhile (duplicate name). "Undo" archives it. */
    private fun reopen(tagId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val tag = current.archivedTags.firstOrNull { it.id == tagId } ?: return
        undo.commit()
        _state.update { it.copy(actionError = null) }
        viewModelScope.launch {
            tags.unarchiveTag(tag.id)
                .onSuccess {
                    _state.update {
                        it.copy(tags = (it.tags + tag.copy(archivedAt = null)).sortedByName(), archivedTags = it.archivedTags - tag)
                    }
                    undo.show(
                        UndoableAction(
                            message = Res.string.tag_reopened,
                            argument = tag.name,
                            commit = { Result.success(Unit) },
                            undo = { tags.archiveTag(tag.id) },
                            restore = { load() },
                        ),
                    )
                }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
        }
    }
}
