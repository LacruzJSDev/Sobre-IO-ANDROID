package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeGroupRepository(var groups: List<Group> = emptyList()) : GroupRepository {

    val calls = mutableListOf<String>()

    var groupsResult: Result<List<Group>>? = null
    var createResult: Result<String> = Result.success("new-id")
    var joinResult: Result<String> = Result.success("joined-id")

    override suspend fun getGroups(): Result<List<Group>> {
        calls += "getGroups()"
        return groupsResult ?: Result.success(groups)
    }

    override suspend fun getGroup(id: String): Result<Group> {
        calls += "getGroup($id)"
        return groups.firstOrNull { it.id == id }?.let { Result.success(it) } ?: Result.failure(AppError.NotFound)
    }

    override suspend fun createGroup(name: String, currencyCode: String): Result<String> {
        calls += "createGroup($name, $currencyCode)"
        return createResult
    }

    override suspend fun joinGroup(code: String): Result<String> {
        calls += "joinGroup($code)"
        return joinResult
    }
}
