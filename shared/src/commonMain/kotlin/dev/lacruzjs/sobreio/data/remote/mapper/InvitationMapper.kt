package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.InvitationStatus
import kotlin.time.Instant

internal fun InvitationDto.toDomain(): Invitation = Invitation(
    id = id,
    code = code,
    role = role.toGroupRole(),
    expiresAt = Instant.parse(expiresAt),
    status = if (status == "accepted") InvitationStatus.Accepted else InvitationStatus.Pending,
    acceptedBy = acceptedBy,
    acceptedAt = acceptedAt?.let(Instant::parse),
)
