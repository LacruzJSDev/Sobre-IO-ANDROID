package dev.lacruzjs.sobreio.ui.auth.signin

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.domain.error.AppError

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    /** Google sign-in needs a web client id in local.properties; without it the button is hidden. */
    val isGoogleAvailable: Boolean = false,
    /** Email to confirm, set when the screen wants the code screen shown. The Route consumes it. */
    val navigateToConfirm: String? = null,
)
