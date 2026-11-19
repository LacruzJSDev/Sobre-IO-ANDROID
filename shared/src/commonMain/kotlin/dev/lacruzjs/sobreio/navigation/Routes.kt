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

/** The tags of a group: count of movements, create, edit, merge, delete (level 3 of the Group tab). Opened from the group settings. */
@Serializable
data class Tags(val groupId: String, val startWithCreate: Boolean = false) : NavKey

/**
 * The movements of one tag (level 4, above [Tags]): the Movements list filtered by the tag, with its totals and report
 * (D-54). Opened by tapping a tag.
 */
@Serializable
data class TagMovements(val groupId: String, val tagId: String) : NavKey

/**
 * The due dates waiting for review (level 2 of the Group tab): confirm or skip each one. Opened by the "Review" button of
 * the Group tab's review card; back returns to [Group], which reads its figures again.
 */
@Serializable
data class GroupReview(val groupId: String) : NavKey

/** Create (`groupId` null) or edit a group, as a full screen. Opened from Groups ("+") and the group settings ("Edit"). */
@Serializable
data class GroupForm(val groupId: String?) : NavKey

/**
 * One account (level 2 of the Accounts tab): top tabs Summary and Commitments. Opened by tapping an account row; back goes
 * to [Group]. The pencil opens [AccountForm], and "See all" opens [AccountMovements] above it.
 */
@Serializable
data class AccountDetail(val groupId: String, val accountId: String) : NavKey

/**
 * The movements of one account (level 3 of the Accounts tab), opened by "See all" in the account's detail: the Movements
 * list with the account fixed. Back returns to the [AccountDetail].
 */
@Serializable
data class AccountMovements(val groupId: String, val accountId: String, val tagId: String? = null) : NavKey

/** Create (`accountId` null) or edit an account, as a full screen. Opened from the Accounts tab. */
/** Level 3 of Accounts, full screen: import a bank statement into the account (D-52). */
@Serializable
data class BankImport(val groupId: String, val accountId: String) : NavKey

@Serializable
data class AccountForm(val groupId: String, val accountId: String?) : NavKey

/**
 * Read only detail of one commitment as an account sees it (level 3 of the Accounts tab): resolved due items and
 * versions. Opened by tapping a row of the Commitments tab; the pencil opens [CommitmentForm]; back returns to the detail.
 */
@Serializable
data class CommitmentDetail(val groupId: String, val accountId: String, val commitmentId: String) : NavKey

/**
 * Create (`commitmentId` null) or edit a commitment of one account, as a full screen (level 3 of the Accounts tab).
 * Opened from the Commitments tab of the account's detail; back returns to it, which reloads the list.
 */
@Serializable
data class CommitmentForm(val groupId: String, val accountId: String, val commitmentId: String?) : NavKey

/**
 * Create (`movementId` null) or edit a movement, as a full screen. [source] is the [dev.lacruzjs.sobreio.domain.model.MovementSource]
 * name of the row being edited; [accountId] preselects the account when creating from an account's list. Opened by the "+"
 * of the Movements tab and of an account's movements, and by the Edit swipe action of a row.
 *
 * The `prefill*` fields are optional and only apply when creating: the form opens with that kind ([prefillKind] is an
 * [dev.lacruzjs.sobreio.domain.model.EntryKind] name), source and destination account and amount already chosen (the Group
 * tab's transfer suggestions). The user reviews and saves; nothing is written until then.
 */
@Serializable
data class MovementForm(
    val groupId: String,
    val movementId: String?,
    val source: String?,
    val accountId: String?,
    val prefillKind: String? = null,
    val prefillFromAccountId: String? = null,
    val prefillToAccountId: String? = null,
    val prefillAmountCents: Long? = null,
) : NavKey

/**
 * One envelope (level 2 of the Envelopes tab), also the system "Unassigned" one, which is read only. Opened by tapping an
 * envelope row; back goes to [Group]. Read only below the pencil, which opens [EnvelopeForm]: the icons of its moves and
 * latest movements sections open [EnvelopeTransfersOf] and [EnvelopeMovements] above it, and a funding commitment opens
 * [CommitmentDetail].
 */
@Serializable
data class EnvelopeDetail(val groupId: String, val envelopeId: String) : NavKey

/**
 * The movements of one envelope (level 3 of the Envelopes tab), opened by "See all" in its detail: the Movements list
 * with the envelope fixed. Back returns to the [EnvelopeDetail].
 */
@Serializable
data class EnvelopeMovements(val groupId: String, val envelopeId: String) : NavKey

/**
 * Every move between envelopes of the group (level 2 of the Envelopes tab): edit, delete with Undo and "+" for a new
 * one. Opened by the moves button of the Envelopes tab header; back goes to [Group].
 */
@Serializable
data class EnvelopeTransfers(val groupId: String) : NavKey

/**
 * The moves of one envelope (level 3 of the Envelopes tab), opened by the icon of the moves section in its detail: the
 * same screen as [EnvelopeTransfers], limited to the envelope, where they are edited and deleted (the detail only shows
 * the latest). Back returns to the [EnvelopeDetail].
 */
@Serializable
data class EnvelopeTransfersOf(val groupId: String, val envelopeId: String) : NavKey

/**
 * Record (`transferId` null) or edit a move between envelopes, as a full screen, opened from [EnvelopeTransfers] or
 * [EnvelopeTransfersOf] (a new one there starts with that envelope as the destination, [toEnvelopeId]); back returns
 * there, which reads its list again.
 */
@Serializable
data class EnvelopeTransferForm(val groupId: String, val transferId: String?, val toEnvelopeId: String? = null) : NavKey


/** Create (`envelopeId` null) or edit an envelope, as a full screen. Opened from the Envelopes tab. */
@Serializable
data class EnvelopeForm(val groupId: String, val envelopeId: String?) : NavKey

/** Choose a time zone for [GroupForm]; [current] is the one to show first and highlighted. */
@Serializable
data class TimeZonePicker(val current: String) : NavKey

@Serializable
data object Profile : NavKey

/** Delete the signed-in user's account: what happens to each group and the final button. Opened from [Profile]. */
@Serializable
data object DeleteAccount : NavKey
