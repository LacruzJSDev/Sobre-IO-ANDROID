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

/** The columns of `envelopes` that the `envelope_balances` view does not have (colour, icon, type, first target day). */
@Serializable
internal data class EnvelopeStyleDto(
    val id: String,
    val color: String? = null,
    val icon: String? = null,
    val purpose: String? = null,
    @SerialName("targets_from") val targetsFrom: String? = null,
)

/** A row of the `envelope_progress` function; dates travel as ISO text. */
@Serializable
internal data class EnvelopeProgressDto(
    @SerialName("envelope_id") val envelopeId: String,
    val purpose: String? = null,
    val state: String,
    val current: Long = 0,
    val goal: Long = 0,
    @SerialName("next_due") val nextDue: String? = null,
    @SerialName("paid_on") val paidOn: String? = null,
    val pending: Int = 0,
    @SerialName("following_due") val followingDue: String? = null,
)

/** The only row of the `envelopes_summary` function. */
@Serializable
internal data class EnvelopesSummaryDto(
    val unassigned: Long = 0,
    @SerialName("in_envelopes_total") val inEnvelopesTotal: Long = 0,
    @SerialName("due_this_period") val dueThisPeriod: Long = 0,
    @SerialName("reserved_later") val reservedLater: Long = 0,
    @SerialName("other_assigned") val otherAssigned: Long = 0,
    val overspent: Long = 0,
    @SerialName("covered_on") val coveredOn: String? = null,
    @SerialName("covering_income") val coveringIncome: Long? = null,
)
