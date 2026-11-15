package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.TagUsage

/** The tags of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface TagRepository {

    /** The live (not archived) tags of the group, ordered by name. Any member can read them. */
    suspend fun getTags(groupId: String): Result<List<Tag>>

    /**
     * Every tag of the group, live and archived, with the number of live movements that carry it (`tag_usage`, counted
     * by the database).
     */
    suspend fun getUsage(groupId: String): Result<List<TagUsage>>

    /**
     * Creates a tag named [name] with the palette colour [color] (a `#RRGGBB` hex, or none) and returns it. Any member
     * may. Fails with [dev.lacruzjs.sobreio.domain.error.AppError.DuplicateName] when a live tag of the group has that
     * name ignoring case, accents and extra spaces.
     */
    suspend fun createTag(groupId: String, name: String, color: String? = null): Result<Tag>

    /** Changes the name and colour of a tag; the same duplicate rule as [createTag] applies. */
    suspend fun updateTag(tagId: String, name: String, color: String?): Result<Unit>

    /** Archives a tag (its movements keep it). */
    suspend fun archiveTag(tagId: String): Result<Unit>

    /** Reopens an archived tag; fails with duplicate name when a live tag took its name meanwhile. */
    suspend fun unarchiveTag(tagId: String): Result<Unit>

    /**
     * `merge_tags`: every movement of [sourceId] carries [targetId] instead (without repeating it) and the source is
     * archived. Both must be live tags of the same group.
     */
    suspend fun mergeTags(sourceId: String, targetId: String): Result<Unit>
}
