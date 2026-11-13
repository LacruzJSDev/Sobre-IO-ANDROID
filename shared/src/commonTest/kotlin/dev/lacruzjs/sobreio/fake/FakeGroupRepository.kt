package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Invitation
import kotlin.time.Instant

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeGroupRepository(var groups: List<Group> = emptyList()) : GroupRepository {

    val calls = mutableListOf<String>()

    var groupsResult: Result<List<Group>>? = null
    var createResult: Result<String> = Result.success("new-id")
    var joinResult: Result<String> = Result.success("joined-id")
    var renameResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var cancelDeletionResult: Result<Unit> = Result.success(Unit)
    var leaveResult: Result<Unit> = Result.success(Unit)
    var invitationResult: Result<Invitation> = Result.success(Invitation("ABC123DEF456", Instant.parse("2026-11-20T10:00:00Z")))

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

    override suspend fun renameGroup(id: String, name: String): Result<Unit> {
        calls += "renameGroup($id, $name)"
        return renameResult
    }

    override suspend fun requestGroupDeletion(id: String): Result<Unit> {
        calls += "requestGroupDeletion($id)"
        return deleteResult
    }

    override suspend fun cancelGroupDeletion(id: String): Result<Unit> {
        calls += "cancelGroupDeletion($id)"
        return cancelDeletionResult
    }

    override suspend fun leaveGroup(id: String): Result<Unit> {
        calls += "leaveGroup($id)"
        return leaveResult
    }

    override suspend fun createInvitation(groupId: String): Result<Invitation> {
        calls += "createInvitation($groupId)"
        return invitationResult
    }
}
