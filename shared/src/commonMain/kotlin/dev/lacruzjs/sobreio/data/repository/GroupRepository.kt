package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Invitation

/** The groups the signed-in user belongs to. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface GroupRepository {

    /** Active groups of the user, ordered by name, with the user's role. Groups whose deletion was requested are not listed. */
    suspend fun getGroups(): Result<List<Group>>

    /** Fails with [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when the group does not exist or the user is not a member. */
    suspend fun getGroup(id: String): Result<Group>

    /** Creates a group using the device time zone; returns its id. The currency cannot be changed afterwards. */
    suspend fun createGroup(name: String, currencyCode: String): Result<String>

    /** Joins with an invitation code; returns the group id. */
    suspend fun joinGroup(code: String): Result<String>

    /** Owner or admin only; anyone else fails with a business rule error (FZ016). */
    suspend fun renameGroup(id: String, name: String): Result<Unit>

    /** An owner asks to delete the group: it is purged 7 days later unless an owner cancels, and it stops being listed. */
    suspend fun requestGroupDeletion(id: String): Result<Unit>

    /** Any owner cancels a requested deletion; the group is listed again. */
    suspend fun cancelGroupDeletion(id: String): Result<Unit>

    /** The signed-in user leaves the group. Fails with FZ009 when they are the only owner. */
    suspend fun leaveGroup(id: String): Result<Unit>

    /** Owner or admin only. Creates an invitation for a new member; the database picks the code and a 7-day expiry. */
    suspend fun createInvitation(groupId: String): Result<Invitation>
}
