package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import kotlin.time.Instant

internal fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    currencyCode = currency,
    role = members.firstOrNull()?.role.toGroupRole(),
    color = color,
    icon = icon,
    timezone = timezone ?: Group.DEFAULT_TIME_ZONE,
    periodStartDay = periodStartDay,
    archivedAt = archivedAt?.let(Instant::parse),
    purgeRequestedAt = purgeRequestedAt?.let(Instant::parse),
    activeOwnerCount = owners?.size,
)

internal fun String?.toGroupRole(): GroupRole = when (this) {
    "owner" -> GroupRole.Owner
    "admin" -> GroupRole.Admin
    else -> GroupRole.Member
}

/** The value of `role_enum`. */
internal fun GroupRole.toDbValue(): String = when (this) {
    GroupRole.Owner -> "owner"
    GroupRole.Admin -> "admin"
    GroupRole.Member -> "member"
}
