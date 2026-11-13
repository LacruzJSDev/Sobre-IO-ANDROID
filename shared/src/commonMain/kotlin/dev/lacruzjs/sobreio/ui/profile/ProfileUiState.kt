package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val isSigningOut: Boolean = false,
    val error: AppError? = null,
)
