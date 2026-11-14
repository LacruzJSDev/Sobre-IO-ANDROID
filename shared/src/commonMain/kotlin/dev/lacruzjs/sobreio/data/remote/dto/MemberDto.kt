package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `account_group_members` with the member's profile embedded. */
@Serializable
internal data class MemberDto(
    @SerialName("user_id") val userId: String,
    val role: String,
    val profiles: ProfileNameDto? = null,
)

@Serializable
internal data class ProfileNameDto(@SerialName("display_name") val displayName: String? = null)
