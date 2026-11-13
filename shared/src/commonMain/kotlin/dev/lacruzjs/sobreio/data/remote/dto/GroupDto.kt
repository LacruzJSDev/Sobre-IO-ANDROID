package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.Serializable

/** The columns of `account_groups` the app reads. */
@Serializable
internal data class GroupDto(
    val id: String,
    val name: String,
    val currency: String,
)
