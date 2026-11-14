package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation

/** The groups the signed-in user belongs to. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface GroupRepository {

    /**
     * Every group of the user, ordered by name, with the user's role: live ones, archived ones
     * ([Group.isArchived]) and the ones whose deletion was requested ([Group.isPendingDeletion]). The caller splits them.
     */
    suspend fun getGroups(): Result<List<Group>>

    /**
     * Live or archived group. Fails with [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when the group does not
     * exist, its deletion was requested or the user is not a member.
     */
    suspend fun getGroup(id: String): Result<Group>

    /** Creates a group; returns its id. The currency cannot be changed afterwards. [color] is `#RRGGBB`, [icon] a catalogue key. */
    suspend fun createGroup(
        name: String,
        currencyCode: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<String>

    /** Joins with an invitation code; returns the group id. */
    suspend fun joinGroup(code: String): Result<String>

    /** Owner or admin only; anyone else fails with a business rule error (FZ016). */
    suspend fun updateGroup(
        id: String,
        name: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<Unit>

    /** Owner only (FZ016 otherwise). An archived group is read only until it is reopened. */
    suspend fun archiveGroup(id: String): Result<Unit>

    /** Owner only: makes an archived group live again. */
    suspend fun reopenGroup(id: String): Result<Unit>

    /** An owner asks to delete the group: it is purged 7 days later unless an owner cancels. */
    suspend fun requestGroupDeletion(id: String): Result<Unit>

    /** Any owner cancels a requested deletion; the group is live again. */
    suspend fun cancelGroupDeletion(id: String): Result<Unit>

    /** The signed-in user leaves the group. Fails with FZ009 when they are the only owner. */
    suspend fun leaveGroup(id: String): Result<Unit>

    /** Owner or admin only. Creates an invitation with [role] (member or admin); the database picks the code and a 7-day expiry. */
    suspend fun createInvitation(groupId: String, role: GroupRole): Result<Invitation>

    /**
     * Owner or admin only. Pending and accepted invitations, newest first (revoked ones are not loaded). Pending ones
     * that already expired are included: the caller filters them with [Invitation.isExpired].
     */
    suspend fun getInvitations(groupId: String): Result<List<Invitation>>

    /** Owner or admin only. Revokes a pending invitation for good. */
    suspend fun revokeInvitation(id: String): Result<Unit>
}
