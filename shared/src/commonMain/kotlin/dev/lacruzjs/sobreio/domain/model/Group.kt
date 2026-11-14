package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * A group of people who share accounts and envelopes. [currencyCode] is the ISO 4217 code and never changes.
 * [role] is the signed-in user's role in it. [color] is the `#RRGGBB` the database stores (null for none) and [icon] a
 * catalogue key. [periodStartDay] (1-28) and [timezone] (IANA id) drive the group's periods. A group with
 * [archivedAt] is read only; one with [purgeRequestedAt] is deleted [PURGE_DELAY] after that unless an owner cancels.
 */
data class Group(
    val id: String,
    val name: String,
    val currencyCode: String,
    val role: GroupRole = GroupRole.Member,
    val color: String? = null,
    val icon: String? = null,
    val timezone: String = DEFAULT_TIME_ZONE,
    val periodStartDay: Int = 1,
    val archivedAt: Instant? = null,
    val purgeRequestedAt: Instant? = null,
) {
    val isOwner: Boolean get() = role == GroupRole.Owner
    val isArchived: Boolean get() = archivedAt != null
    val isPendingDeletion: Boolean get() = purgeRequestedAt != null

    /** Owner or admin of a live group: archived groups and groups pending deletion are read only. */
    val canEdit: Boolean
        get() = (role == GroupRole.Owner || role == GroupRole.Admin) && !isArchived && !isPendingDeletion

    /** When the group will be purged, or null when no deletion was requested. */
    val deletionDate: Instant? get() = purgeRequestedAt?.plus(PURGE_DELAY)

    companion object {
        val PURGE_DELAY = 7.days
        const val DEFAULT_TIME_ZONE = "Europe/Madrid"
    }
}
