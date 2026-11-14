package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The columns of `transactions` that the form edits. */
@Serializable
internal data class TransactionRowDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("envelope_id") val envelopeId: String,
    val kind: String,
    val amount: Long,
    val date: String,
    val notes: String? = null,
    @SerialName("voided_at") val voidedAt: String? = null,
)

/** The columns of `account_transfers` that the form edits. */
@Serializable
internal data class TransferRowDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("from_account_id") val fromAccountId: String,
    @SerialName("to_account_id") val toAccountId: String,
    val amount: Long,
    val date: String,
    val notes: String? = null,
    @SerialName("voided_at") val voidedAt: String? = null,
)

@Serializable
internal data class TagLinkDto(@SerialName("tag_id") val tagId: String)

@Serializable
internal data class IdRowDto(val id: String)
