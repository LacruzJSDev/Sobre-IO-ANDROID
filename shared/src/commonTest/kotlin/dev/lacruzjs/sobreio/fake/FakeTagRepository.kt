package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.TagRepository
import dev.lacruzjs.sobreio.domain.model.Tag

class FakeTagRepository(var tags: List<Tag> = emptyList()) : TagRepository {
    override suspend fun getTags(groupId: String): Result<List<Tag>> = Result.success(tags)
}
