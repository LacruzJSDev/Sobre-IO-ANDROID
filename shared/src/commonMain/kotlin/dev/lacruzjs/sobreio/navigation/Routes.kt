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

/**
 * One account (level 2 of the Accounts tab): top tabs Summary and Commitments. Opened by tapping an account row; back goes
 * to [Group]. The pencil opens [AccountForm], and "See all" returns to [Group] on its Movements tab filtered to the account.
 */
@Serializable
data class AccountDetail(val groupId: String, val accountId: String) : NavKey

/** Create (`accountId` null) or edit an account, as a full screen. Opened from the Accounts tab. */
@Serializable
data class AccountForm(val groupId: String, val accountId: String?) : NavKey

/**
 * Create (`commitmentId` null) or edit a commitment of one account, as a full screen (level 3 of the Accounts tab).
 * Opened from the Commitments tab of the account's detail; back returns to it, which reloads the list.
 */
@Serializable
data class CommitmentForm(val groupId: String, val accountId: String, val commitmentId: String?) : NavKey

/** Create (`envelopeId` null) or edit an envelope, as a full screen. Opened from the Envelopes tab. */
@Serializable
data class EnvelopeForm(val groupId: String, val envelopeId: String?) : NavKey

/** Choose a time zone for [GroupForm]; [current] is the one to show first and highlighted. */
@Serializable
data class TimeZonePicker(val current: String) : NavKey

@Serializable
data object Profile : NavKey
