package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `commitment_versions`; dates travel as ISO text. */
@Serializable
internal data class CommitmentVersionDto(
    val id: String,
    @SerialName("commitment_id") val commitmentId: String,
    @SerialName("effective_from") val effectiveFrom: String,
    @SerialName("effective_to") val effectiveTo: String? = null,
    val kind: String,
    val amount: Long = 0,
    @SerialName("account_id") val accountId: String,
    @SerialName("to_account_id") val toAccountId: String? = null,
    @SerialName("envelope_id") val envelopeId: String? = null,
    @SerialName("first_due_date") val firstDueDate: String,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("every_n") val everyN: Int? = null,
    val unit: String? = null,
    @SerialName("anchor_day") val anchorDay: Int? = null,
    @SerialName("end_date") val endDate: String? = null,
    val funded: Boolean = false,
    @SerialName("auto_register") val autoRegister: Boolean = false,
    val notes: String? = null,
)

/** A row of `commitments`. */
@Serializable
internal data class CommitmentDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val name: String,
    val notes: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
)

/** The commitment id of a version row, to know which commitments touch an account. */
@Serializable
internal data class CommitmentIdDto(@SerialName("commitment_id") val commitmentId: String)

/** A row of `commitment_links`. */
@Serializable
internal data class CommitmentLinkDto(
    val id: String,
    @SerialName("out_group_id") val outGroupId: String,
    @SerialName("in_group_id") val inGroupId: String,
    @SerialName("out_commitment_id") val outCommitmentId: String,
    @SerialName("in_commitment_id") val inCommitmentId: String,
)

/** A commitment with its versions embedded (`commitment_versions(...)`). */
@Serializable
internal data class CommitmentWithVersionsDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val name: String,
    val notes: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    @SerialName("commitment_versions") val versions: List<CommitmentVersionDto> = emptyList(),
) {
    fun header() = CommitmentDto(id, groupId, name, notes, archivedAt)
}

/** The movement an occurrence registered, embedded as `transactions(...)` or `account_transfers(...)`. */
@Serializable
internal data class OccurrenceFactDto(
    val amount: Long,
    val date: String,
    @SerialName("from_account_id") val fromAccountId: String? = null,
)

/** A row of `commitment_occurrences` with its movement embedded. */
@Serializable
internal data class CommitmentOccurrenceDto(
    @SerialName("due_date") val dueDate: String,
    val status: String,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("transfer_id") val transferId: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    val transactions: OccurrenceFactDto? = null,
    @SerialName("account_transfers") val transfer: OccurrenceFactDto? = null,
)
