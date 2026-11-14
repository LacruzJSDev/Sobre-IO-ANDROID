package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member

class FakeMemberRepository(var members: List<Member> = emptyList()) : MemberRepository {

    val calls = mutableListOf<String>()

    var result: Result<List<Member>>? = null
    var changeRoleResult: Result<Unit> = Result.success(Unit)
    var removeResult: Result<Unit> = Result.success(Unit)

    override suspend fun getMembers(groupId: String): Result<List<Member>> = result ?: Result.success(members)

    override suspend fun changeRole(groupId: String, userId: String, role: GroupRole): Result<Unit> {
        calls += "changeRole($groupId, $userId, $role)"
        return changeRoleResult
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        calls += "removeMember($groupId, $userId)"
        return removeResult
    }
}
