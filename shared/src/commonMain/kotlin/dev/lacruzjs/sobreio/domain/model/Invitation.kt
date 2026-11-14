package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Instant

/** `invitation_status_enum` values the app shows: revoked invitations are never loaded. */
enum class InvitationStatus { Pending, Accepted }

/**
 * A code that lets someone join a group with [role] until [expiresAt]. [id] is what revoking needs. An accepted
 * invitation says who ([acceptedBy], a user id) and when ([acceptedAt]).
 */
data class Invitation(
    val id: String,
    val code: String,
    val role: GroupRole,
    val expiresAt: Instant,
    val status: InvitationStatus = InvitationStatus.Pending,
    val acceptedBy: String? = null,
    val acceptedAt: Instant? = null,
) {
    fun isExpired(now: Instant): Boolean = expiresAt <= now
}
