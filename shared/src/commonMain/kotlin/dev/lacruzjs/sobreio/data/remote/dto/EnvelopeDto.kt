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
    @SerialName("monthly_target") val monthlyTarget: Long = 0,
    val balance: Long = 0,
)

/** The envelope columns of the `group_overview` view. */
@Serializable
internal data class EnvelopesOverviewDto(
    @SerialName("in_envelopes_total") val inEnvelopesTotal: Long? = null,
    @SerialName("unassigned_balance") val unassignedBalance: Long? = null,
)
