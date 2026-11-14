package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The columns of `invitations` the app reads. */
@Serializable
internal data class InvitationDto(
    val id: String,
    val code: String,
    val role: String = "member",
    @SerialName("expires_at") val expiresAt: String,
)
