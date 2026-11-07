package dev.lacruzjs.sobreio.ui.auth.signin

sealed interface SignInEvent {
    data class EmailChanged(val value: String) : SignInEvent
    data class PasswordChanged(val value: String) : SignInEvent
    data object SignInClicked : SignInEvent

    /** The email is not confirmed yet: send a new code and go to the code screen. */
    data object ConfirmEmailClicked : SignInEvent
    data object GoogleClicked : SignInEvent
    data class GoogleTokenReceived(val idToken: String, val rawNonce: String) : SignInEvent
    data object GoogleFailed : SignInEvent
    data object ForgotPasswordClicked : SignInEvent
    data object CreateAccountClicked : SignInEvent
    data object NavigationHandled : SignInEvent
}
