package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.error.AppError
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
) {
    /** The name differs from the saved one, so there is something to save. */
    val isDirty: Boolean get() = name.trim() != user?.displayName.orEmpty()
}
