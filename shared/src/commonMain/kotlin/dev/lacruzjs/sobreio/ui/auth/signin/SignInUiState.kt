package dev.lacruzjs.sobreio.ui.auth.signin

import dev.lacruzjs.sobreio.domain.error.AppError
import org.jetbrains.compose.resources.StringResource

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val emailError: StringResource? = null,
    val passwordError: StringResource? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    /** Set by the Route: Google sign-in needs a platform launcher and, on Android, a web client id in local.properties. */
    val isGoogleAvailable: Boolean = false,
    /** Email to confirm, set when the screen wants the code screen shown. The Route consumes it. */
    val navigateToConfirm: String? = null,
)
