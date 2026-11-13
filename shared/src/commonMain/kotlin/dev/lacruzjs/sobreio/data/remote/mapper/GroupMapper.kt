package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole

internal fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    currencyCode = currency,
    role = members.firstOrNull()?.role.toGroupRole(),
)

internal fun String?.toGroupRole(): GroupRole = when (this) {
    "owner" -> GroupRole.Owner
    "admin" -> GroupRole.Admin
    else -> GroupRole.Member
}
