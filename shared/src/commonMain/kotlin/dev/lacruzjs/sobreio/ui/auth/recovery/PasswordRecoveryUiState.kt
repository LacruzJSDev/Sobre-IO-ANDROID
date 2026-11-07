package dev.lacruzjs.sobreio.ui.auth.recovery

import dev.lacruzjs.sobreio.domain.error.AppError
import org.jetbrains.compose.resources.StringResource

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
    val emailError: StringResource? = null,
    val codeError: StringResource? = null,
    val newPasswordError: StringResource? = null,
    val repeatedPasswordError: StringResource? = null,
    val isLoading: Boolean = false,
    /** A new code was just sent again, shown until the user edits something. */
    val codeResent: Boolean = false,
    val error: AppError? = null,
)
