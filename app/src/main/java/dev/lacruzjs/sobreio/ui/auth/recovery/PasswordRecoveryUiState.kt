package dev.lacruzjs.sobreio.ui.auth.recovery

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.domain.error.AppError

enum class RecoveryStep {
    /** Ask for the email and send the code. */
    Email,

    /** Ask for the code and the new password. */
    Reset,
}

data class PasswordRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.Email,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val repeatedPassword: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val codeError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val repeatedPasswordError: Int? = null,
    val isLoading: Boolean = false,
    /** A new code was just sent again, shown until the user edits something. */
    val codeResent: Boolean = false,
    val error: AppError? = null,
)
