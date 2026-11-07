package dev.lacruzjs.sobreio.ui.auth.confirm

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.domain.error.AppError

data class ConfirmEmailUiState(
    val email: String,
    val code: String = "",
    @StringRes val codeError: Int? = null,
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    /** Seconds until the code can be requested again; 0 means the resend button is enabled. */
    val resendSecondsLeft: Int = 0,
    /** A new code was just sent, shown as a confirmation until the user edits the code. */
    val codeResent: Boolean = false,
    val error: AppError? = null,
)
