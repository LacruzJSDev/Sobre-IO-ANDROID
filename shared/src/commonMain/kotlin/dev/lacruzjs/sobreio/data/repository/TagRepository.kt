package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Tag

/** The tags of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface TagRepository {

    /** The live (not archived) tags of the group, ordered by name. Any member can read them. */
    suspend fun getTags(groupId: String): Result<List<Tag>>

    /**
     * Creates a tag named [name] and returns it. Any member may. Fails with
     * [dev.lacruzjs.sobreio.domain.error.AppError.DuplicateName] when a live tag of the group has that name (ignoring case).
     */
    suspend fun createTag(groupId: String, name: String): Result<Tag>
}
