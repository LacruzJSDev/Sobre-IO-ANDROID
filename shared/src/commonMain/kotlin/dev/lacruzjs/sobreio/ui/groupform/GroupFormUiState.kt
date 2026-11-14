package dev.lacruzjs.sobreio.ui.groupform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.ui.groups.GroupFormState

data class GroupFormUiState(
    val form: GroupFormState = GroupFormState(),
    /** Editing: the group is being read. Creating: always false. */
    val isLoading: Boolean = false,
    /** Failure of reading the group to edit. */
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    /** Failure of saving, shown inline in the form. */
    val submitError: AppError? = null,
    /** Id of the group just created or saved: the Route leaves the form (and opens the group after creating it). */
    val savedGroupId: String? = null,
)
