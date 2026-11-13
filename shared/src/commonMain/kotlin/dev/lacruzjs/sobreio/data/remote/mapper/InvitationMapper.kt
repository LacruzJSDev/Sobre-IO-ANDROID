package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.domain.model.Invitation
import kotlin.time.Instant

internal fun InvitationDto.toDomain(): Invitation = Invitation(code = code, expiresAt = Instant.parse(expiresAt))
