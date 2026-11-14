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
/** Root of the signed-in back stack: the user's groups. */
@Serializable
data object Groups : NavKey

/** One group, with its four tabs. Back goes to [Groups]. */
@Serializable
data class Group(val groupId: String) : NavKey

@Serializable
data object Profile : NavKey
