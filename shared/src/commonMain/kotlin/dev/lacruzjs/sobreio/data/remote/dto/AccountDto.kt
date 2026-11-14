package dev.lacruzjs.sobreio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of the `account_balances` view: the account and the balance in cents that the database computed. */
@Serializable
internal data class AccountDto(
    @SerialName("account_id") val id: String,
    @SerialName("group_id") val groupId: String,
    val name: String,
    val kind: String,
    @SerialName("in_envelopes") val inEnvelopes: Boolean = false,
    val balance: Long = 0,
)

/** The `accounts_total` column of the `group_overview` view. */
@Serializable
internal data class AccountsTotalDto(@SerialName("accounts_total") val total: Long? = null)
