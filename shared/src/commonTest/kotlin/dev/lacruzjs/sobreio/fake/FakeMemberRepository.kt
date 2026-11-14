package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.model.Member

class FakeMemberRepository(var members: List<Member> = emptyList()) : MemberRepository {

    var result: Result<List<Member>>? = null

    override suspend fun getMembers(groupId: String): Result<List<Member>> = result ?: Result.success(members)
}
