package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.MemberDto
import dev.lacruzjs.sobreio.domain.model.Member

internal fun MemberDto.toDomain(): Member = Member(
    userId = userId,
    displayName = profiles?.displayName.orEmpty(),
    role = role.toGroupRole(),
)
