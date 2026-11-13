package dev.lacruzjs.sobreio.ui.auth.signup

import dev.lacruzjs.sobreio.domain.error.AppError
import org.jetbrains.compose.resources.StringResource

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val nameError: StringResource? = null,
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    /** Email to confirm once the account is created. The Route consumes it. */
    val navigateToConfirm: String? = null,
)
