package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Tag

/** The tags of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface TagRepository {

    /** The live (not archived) tags of the group, ordered by name. Any member can read them. */
    suspend fun getTags(groupId: String): Result<List<Tag>>
}
