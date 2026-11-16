package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `account_deletion_preview()`. */
@Serializable
internal data class DeletionPreviewDto(
    @SerialName("group_id") val groupId: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val outcome: String,
)
