package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.model.Tag

class FakeTagRepository(var tags: List<Tag> = emptyList()) : TagRepository {
    var createResult: Result<Tag>? = null
    val created = mutableListOf<String>()

    override suspend fun getTags(groupId: String): Result<List<Tag>> = Result.success(tags)

    override suspend fun createTag(groupId: String, name: String): Result<Tag> {
        created += name
        return createResult ?: Result.success(Tag("tag-${created.size}", groupId, name))
    }
}
