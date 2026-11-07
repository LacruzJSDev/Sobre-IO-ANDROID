package dev.lacruzjs.sobreio.ui.auth.signup

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.domain.error.AppError

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    /** Email to confirm once the account is created. The Route consumes it. */
    val navigateToConfirm: String? = null,
)
