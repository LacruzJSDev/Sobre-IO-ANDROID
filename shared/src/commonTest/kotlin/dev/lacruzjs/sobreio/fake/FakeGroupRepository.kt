package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import kotlin.time.Instant

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeGroupRepository(var groups: List<Group> = emptyList()) : GroupRepository {

    val calls = mutableListOf<String>()

    var groupsResult: Result<List<Group>>? = null
    var createResult: Result<String> = Result.success("new-id")
    var joinResult: Result<String> = Result.success("joined-id")
    var updateResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var reopenResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var cancelDeletionResult: Result<Unit> = Result.success(Unit)
    var leaveResult: Result<Unit> = Result.success(Unit)
    var invitationResult: Result<Invitation> =
        Result.success(Invitation("inv-new", "ABC123DEF456", GroupRole.Member, Instant.parse("2026-11-20T10:00:00Z")))
    var invitations: List<Invitation> = emptyList()
    var revokeResult: Result<Unit> = Result.success(Unit)

    override suspend fun getGroups(): Result<List<Group>> {
        calls += "getGroups()"
        return groupsResult ?: Result.success(groups)
    }

    override suspend fun getGroup(id: String): Result<Group> {
        calls += "getGroup($id)"
        return groups.firstOrNull { it.id == id && !it.isPendingDeletion }?.let { Result.success(it) }
            ?: Result.failure(AppError.NotFound)
    }

    override suspend fun createGroup(
        name: String,
        currencyCode: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<String> {
        calls += "createGroup($name, $currencyCode, $timezone, $periodStartDay, $color, $icon)"
        return createResult
    }

    override suspend fun joinGroup(code: String): Result<String> {
        calls += "joinGroup($code)"
        return joinResult
    }

    override suspend fun updateGroup(
        id: String,
        name: String,
        timezone: String,
        periodStartDay: Int,
        color: String?,
        icon: String?,
    ): Result<Unit> {
        calls += "updateGroup($id, $name, $timezone, $periodStartDay, $color, $icon)"
        return updateResult
    }

    override suspend fun archiveGroup(id: String): Result<Unit> {
        calls += "archiveGroup($id)"
        if (archiveResult.isSuccess) {
            groups = groups.map { if (it.id == id) it.copy(archivedAt = Instant.parse("2026-11-14T10:00:00Z")) else it }
        }
        return archiveResult
    }

    override suspend fun reopenGroup(id: String): Result<Unit> {
        calls += "reopenGroup($id)"
        if (reopenResult.isSuccess) groups = groups.map { if (it.id == id) it.copy(archivedAt = null) else it }
        return reopenResult
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

    override suspend fun createInvitation(groupId: String, role: GroupRole): Result<Invitation> {
        calls += "createInvitation($groupId, $role)"
        return invitationResult.map { it.copy(role = role) }
    }

    override suspend fun getPendingInvitations(groupId: String): Result<List<Invitation>> {
        calls += "getPendingInvitations($groupId)"
        return Result.success(invitations)
    }

    override suspend fun revokeInvitation(id: String): Result<Unit> {
        calls += "revokeInvitation($id)"
        return revokeResult
    }
}
