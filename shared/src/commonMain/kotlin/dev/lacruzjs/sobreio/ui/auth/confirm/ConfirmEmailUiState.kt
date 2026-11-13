package dev.lacruzjs.sobreio.ui.auth.confirm

import dev.lacruzjs.sobreio.domain.error.AppError
import org.jetbrains.compose.resources.StringResource

data class ConfirmEmailUiState(
    val email: String,
    val code: String = "",
    val codeError: StringResource? = null,
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    /** Seconds until the code can be requested again; 0 means the resend button is enabled. */
    val resendSecondsLeft: Int = 0,
    /** A new code was just sent, shown as a confirmation until the user edits the code. */
    val codeResent: Boolean = false,
    val error: AppError? = null,
)
