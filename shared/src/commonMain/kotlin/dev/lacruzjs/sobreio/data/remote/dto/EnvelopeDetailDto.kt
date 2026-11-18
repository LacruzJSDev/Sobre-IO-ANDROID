package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of the `envelope_periods` view; dates travel as ISO text. */
@Serializable
internal data class EnvelopePeriodDto(
    @SerialName("period_id") val periodId: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("opening_balance") val openingBalance: Long = 0,
    val assigned: Long = 0,
    val activity: Long = 0,
    @SerialName("closing_balance") val closingBalance: Long = 0,
)

/** A row of the `envelope_outlook_current` view. */
@Serializable
internal data class EnvelopeOutlookDto(
    val balance: Long = 0,
    val committed: Long = 0,
    val free: Long = 0,
    val shortfall: Long = 0,
)

/** A row of `envelope_transfers`; [createdAt] is the timestamp text. */
@Serializable
internal data class EnvelopeTransferDto(
    val id: String,
    @SerialName("from_envelope_id") val fromEnvelopeId: String,
    @SerialName("to_envelope_id") val toEnvelopeId: String,
    val amount: Long,
    val origin: String,
    @SerialName("commitment_id") val commitmentId: String? = null,
    @SerialName("created_at") val createdAt: String,
    val notes: String? = null,
)

/** A funded version of a commitment that points to an envelope. */
@Serializable
internal data class FundedVersionDto(
    @SerialName("commitment_id") val commitmentId: String,
    @SerialName("account_id") val accountId: String,
    val kind: String,
    @SerialName("effective_from") val effectiveFrom: String,
)

/** A commitment's id and name, read apart from the rows that point to it. */
@Serializable
internal data class LiveCommitmentDto(val id: String, val name: String)
