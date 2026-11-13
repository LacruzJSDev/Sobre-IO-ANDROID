package dev.lacruzjs.sobreio.ui.auth.signup

sealed interface SignUpEvent {
    data class NameChanged(val value: String) : SignUpEvent
    data class EmailChanged(val value: String) : SignUpEvent
    data class PasswordChanged(val value: String) : SignUpEvent
    data object SignUpClicked : SignUpEvent
    data object SignInClicked : SignUpEvent
    data object NavigationHandled : SignUpEvent
}
