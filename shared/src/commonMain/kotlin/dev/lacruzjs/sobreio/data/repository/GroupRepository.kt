package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Group

/** The groups the signed-in user belongs to. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface GroupRepository {

    /** Active groups of the user, ordered by name. */
    suspend fun getGroups(): Result<List<Group>>

    /** Fails with [dev.lacruzjs.sobreio.domain.error.AppError.NotFound] when the group does not exist or the user is not a member. */
    suspend fun getGroup(id: String): Result<Group>

    /** Creates a group using the device time zone; returns its id. The currency cannot be changed afterwards. */
    suspend fun createGroup(name: String, currencyCode: String): Result<String>

    /** Joins with an invitation code; returns the group id. */
    suspend fun joinGroup(code: String): Result<String>
}
