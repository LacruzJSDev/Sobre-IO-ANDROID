package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The columns of `account_groups` the app reads, with the user's own membership embedded. The query filters the
 * embedded rows to the signed-in user, so [members] holds exactly one entry.
 */
@Serializable
internal data class GroupDto(
    val id: String,
    val name: String,
    val currency: String,
    val timezone: String? = null,
    @SerialName("period_start_day") val periodStartDay: Int = 1,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("purge_requested_at") val purgeRequestedAt: String? = null,
    @SerialName("account_group_members") val members: List<MemberRoleDto> = emptyList(),
)

@Serializable
internal data class MemberRoleDto(val role: String)
