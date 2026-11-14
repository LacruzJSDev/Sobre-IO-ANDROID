package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `search_movements`. */
@Serializable
internal data class MovementSearchDto(
    val id: String,
    val source: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("counter_account_id") val counterAccountId: String? = null,
    @SerialName("envelope_id") val envelopeId: String? = null,
    val kind: String,
    val amount: Long = 0,
    val date: String,
    val notes: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("tag_ids") val tagIds: List<String> = emptyList(),
)

/** A row of `tags`. */
@Serializable
internal data class TagDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val name: String,
    val color: String? = null,
)

/** The columns of `periods` that the Movements screen reads. */
@Serializable
internal data class PeriodDto(
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
)
