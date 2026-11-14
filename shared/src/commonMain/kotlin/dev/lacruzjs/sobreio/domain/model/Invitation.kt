package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Instant

/** A code that lets someone join a group with [role] until [expiresAt]. [id] is what revoking needs. */
data class Invitation(
    val id: String,
    val code: String,
    val role: GroupRole,
    val expiresAt: Instant,
) {
    fun isExpired(now: Instant): Boolean = expiresAt <= now
}
