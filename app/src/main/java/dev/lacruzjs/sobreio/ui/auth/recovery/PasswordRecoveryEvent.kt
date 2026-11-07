package dev.lacruzjs.sobreio.ui.auth.recovery

sealed interface PasswordRecoveryEvent {
    data class EmailChanged(val value: String) : PasswordRecoveryEvent
    data object SendCodeClicked : PasswordRecoveryEvent
    data object ResendCodeClicked : PasswordRecoveryEvent

    /** Back from the code step to the email step. */
    data object ChangeEmailClicked : PasswordRecoveryEvent
    data class CodeChanged(val value: String) : PasswordRecoveryEvent
    data class NewPasswordChanged(val value: String) : PasswordRecoveryEvent
    data class RepeatedPasswordChanged(val value: String) : PasswordRecoveryEvent
    data object ResetClicked : PasswordRecoveryEvent
    data object BackToSignInClicked : PasswordRecoveryEvent
}
