package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Signed-out destinations
@Serializable
data object SignIn : NavKey

@Serializable
data object SignUp : NavKey

@Serializable
data class ConfirmEmail(val email: String) : NavKey

@Serializable
data object PasswordRecovery : NavKey

// Signed-in destinations
/** Temporary start of the signed-in app; the Groups screen takes its place. */
@Serializable
data object Home : NavKey
