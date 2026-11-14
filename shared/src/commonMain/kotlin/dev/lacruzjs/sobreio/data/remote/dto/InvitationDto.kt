package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The columns of `invitations` the app reads. */
@Serializable
internal data class InvitationDto(
    val id: String,
    val code: String,
    val role: String = "member",
    val status: String = "pending",
    @SerialName("accepted_by") val acceptedBy: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String,
)
