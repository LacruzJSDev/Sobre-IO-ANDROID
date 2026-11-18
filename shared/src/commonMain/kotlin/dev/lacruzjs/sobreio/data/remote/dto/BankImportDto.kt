package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of `bank_import_preview`. */
@Serializable
internal data class BankRowPreviewDto(
    val idx: Int,
    val status: String,
    @SerialName("suggested_action") val suggestedAction: String,
    @SerialName("commitment_id") val commitmentId: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("match_source") val matchSource: String? = null,
    @SerialName("match_id") val matchId: String? = null,
    @SerialName("envelope_id") val envelopeId: String? = null,
    val pending: List<PendingCandidateDto> = emptyList(),
    val existing: List<ExistingCandidateDto> = emptyList(),
)

@Serializable
internal data class PendingCandidateDto(
    @SerialName("commitment_id") val commitmentId: String,
    @SerialName("commitment_name") val name: String,
    @SerialName("due_date") val dueDate: String,
    val amount: Long,
)

@Serializable
internal data class ExistingCandidateDto(
    val source: String,
    val id: String,
    val kind: String,
    val date: String,
    val amount: Long,
    val notes: String? = null,
)

/** A row of `bank_import_overview`. */
@Serializable
internal data class BankImportOverviewDto(
    val id: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("tag_id") val tagId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("voided_at") val voidedAt: String? = null,
    @SerialName("rows_total") val rowsTotal: Int = 0,
    @SerialName("created_count") val created: Int = 0,
    @SerialName("confirmed_count") val confirmed: Int = 0,
    @SerialName("linked_count") val linked: Int = 0,
    @SerialName("ignored_count") val ignored: Int = 0,
    @SerialName("first_date") val firstDate: String? = null,
    @SerialName("last_date") val lastDate: String? = null,
)
