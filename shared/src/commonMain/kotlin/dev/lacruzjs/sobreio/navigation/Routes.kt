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

/** Details, members and invitations of a group (level 2 of the Group tab). Back goes to [Group]. */
@Serializable
data class GroupSettings(val groupId: String) : NavKey

/** Create (`groupId` null) or edit a group, as a full screen. Opened from Groups ("+") and the group settings ("Edit"). */
@Serializable
data class GroupForm(val groupId: String?) : NavKey

/** Create (`accountId` null) or edit an account, as a full screen. Opened from the Accounts tab. */
@Serializable
data class AccountForm(val groupId: String, val accountId: String?) : NavKey

/** Create (`envelopeId` null) or edit an envelope, as a full screen. Opened from the Envelopes tab. */
@Serializable
data class EnvelopeForm(val groupId: String, val envelopeId: String?) : NavKey

/** Choose a time zone for [GroupForm]; [current] is the one to show first and highlighted. */
@Serializable
data class TimeZonePicker(val current: String) : NavKey

@Serializable
data object Profile : NavKey
