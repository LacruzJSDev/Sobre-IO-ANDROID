package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The columns of `invitations` the app reads back after creating one. */
@Serializable
internal data class InvitationDto(
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
)
