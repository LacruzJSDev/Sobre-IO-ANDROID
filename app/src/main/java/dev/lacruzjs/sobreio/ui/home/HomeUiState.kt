package dev.lacruzjs.sobreio.ui.home

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.User

data class HomeUiState(
    val user: User? = null,
    val isSigningOut: Boolean = false,
    val error: AppError? = null,
)
