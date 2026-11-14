package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of the `envelope_balances` view: the envelope and the balance in cents that the database computed. */
@Serializable
internal data class EnvelopeDto(
    @SerialName("envelope_id") val id: String,
    @SerialName("group_id") val groupId: String,
    val name: String,
    val kind: String,
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("monthly_target") val monthlyTarget: Long = 0,
    val balance: Long = 0,
)

/** The columns of `envelopes` that the `envelope_balances` view does not have. */
@Serializable
internal data class EnvelopeStyleDto(val id: String, val color: String? = null, val icon: String? = null)

/** The envelope columns of the `group_overview` view. */
@Serializable
internal data class EnvelopesOverviewDto(
    @SerialName("in_envelopes_total") val inEnvelopesTotal: Long? = null,
    @SerialName("unassigned_balance") val unassignedBalance: Long? = null,
)
