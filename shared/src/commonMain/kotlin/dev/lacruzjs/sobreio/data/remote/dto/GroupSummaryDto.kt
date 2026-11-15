package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `group_outlook_current`. */
@Serializable
internal data class GroupOutlookDto(
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    val unassigned: Long = 0,
    val overspent: Long = 0,
    @SerialName("free_today") val freeToday: Long = 0,
    @SerialName("committed_shortfall") val committedShortfall: Long = 0,
    @SerialName("pending_out") val pendingOut: Long = 0,
    @SerialName("pending_income") val pendingIncome: Long = 0,
    @SerialName("free_projected") val freeProjected: Long = 0,
)

/** A row of `pending_occurrences` or `upcoming_occurrences`. */
@Serializable
internal data class DueOccurrenceDto(
    @SerialName("commitment_id") val commitmentId: String,
    @SerialName("version_id") val versionId: String = "",
    @SerialName("commitment_name") val name: String,
    val kind: String,
    @SerialName("due_date") val dueDate: String,
    val amount: Long = 0,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("to_account_id") val toAccountId: String? = null,
    @SerialName("envelope_id") val envelopeId: String? = null,
    @SerialName("auto_register") val autoRegister: Boolean = false,
)
