package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.TagUsage

class FakeTagRepository(
    var tags: List<Tag> = emptyList(),
    var usage: List<TagUsage> = emptyList(),
) : TagRepository {
    var tagsResult: Result<List<Tag>>? = null
    var createResult: Result<Tag>? = null
    var usageResult: Result<List<TagUsage>>? = null
    var updateResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var unarchiveResult: Result<Unit> = Result.success(Unit)
    var mergeResult: Result<Unit> = Result.success(Unit)
    val created = mutableListOf<String>()
    val createdColors = mutableListOf<String?>()
    val calls = mutableListOf<String>()

    override suspend fun getTags(groupId: String): Result<List<Tag>> = tagsResult ?: Result.success(tags)

    override suspend fun getUsage(groupId: String): Result<List<TagUsage>> = usageResult ?: Result.success(usage)

    override suspend fun createTag(groupId: String, name: String, color: String?): Result<Tag> {
        created += name
        createdColors += color
        calls += "createTag($name)"
        return createResult ?: Result.success(Tag("tag-${created.size}", groupId, name, color))
    }

    override suspend fun updateTag(tagId: String, name: String, color: String?): Result<Unit> {
        calls += "updateTag($tagId,$name,$color)"
        return updateResult
    }

    override suspend fun archiveTag(tagId: String): Result<Unit> {
        calls += "archiveTag($tagId)"
        return archiveResult
    }

    override suspend fun unarchiveTag(tagId: String): Result<Unit> {
        calls += "unarchiveTag($tagId)"
        return unarchiveResult
    }

    override suspend fun mergeTags(sourceId: String, targetId: String): Result<Unit> {
        calls += "mergeTags($sourceId,$targetId)"
        return mergeResult
    }
}
