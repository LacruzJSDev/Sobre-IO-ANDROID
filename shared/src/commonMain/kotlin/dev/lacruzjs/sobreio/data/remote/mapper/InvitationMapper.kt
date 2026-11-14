package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.domain.model.Invitation
import kotlin.time.Instant

internal fun InvitationDto.toDomain(): Invitation =
    Invitation(id = id, code = code, role = role.toGroupRole(), expiresAt = Instant.parse(expiresAt))
