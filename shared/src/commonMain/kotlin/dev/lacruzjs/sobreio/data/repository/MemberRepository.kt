package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member

/** The members of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface MemberRepository {

    /** Current members (not the ones who left), ordered by name. Any member can read them. */
    suspend fun getMembers(groupId: String): Result<List<Member>>

    /** Owner only (FZ016 otherwise). Fails with FZ009 when it would leave the group without an owner. */
    suspend fun changeRole(groupId: String, userId: String, role: GroupRole): Result<Unit>

    /** Owner only (FZ016 otherwise): sets `left_at` of another member. Fails with FZ009 for the last owner. */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
}
