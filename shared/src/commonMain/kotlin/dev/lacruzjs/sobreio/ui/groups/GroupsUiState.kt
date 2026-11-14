package dev.lacruzjs.sobreio.ui.groups

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.StringResource

data class GroupsUiState(
    /** Live groups, the main list. */
    val groups: List<Group> = emptyList(),
    /** The "Archived" section: archived groups first, then the ones whose deletion was requested, each by name. */
    val archivedGroups: List<Group> = emptyList(),
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading the list. */
    val error: AppError? = null,
    val sheet: GroupsSheet = GroupsSheet.None,
    /** The fields of the Create and Edit sheets. */
    val form: GroupFormState = GroupFormState(),
    val code: String = "",
    val codeError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of creating, editing or joining, shown inside the open sheet. */
    val submitError: AppError? = null,
    val isSigningOut: Boolean = false,
    /** Failure of signing out, shown above the bottom section. */
    val signOutError: AppError? = null,
    /** Failure of deleting, leaving, reopening or undoing, shown above the bottom section. */
    val actionError: AppError? = null,
    /** Snackbar with "Undo" for the last delete or leave. */
    val undoMessage: UndoMessage? = null,
    /** Id of the group to open after creating or joining one. The Route consumes it. */
    val navigateToGroup: String? = null,
)
