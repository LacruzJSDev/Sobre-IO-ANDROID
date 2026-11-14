package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Member

/** The members of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface MemberRepository {

    /** Current members (not the ones who left), ordered by name. Any member can read them. */
    suspend fun getMembers(groupId: String): Result<List<Member>>
}
