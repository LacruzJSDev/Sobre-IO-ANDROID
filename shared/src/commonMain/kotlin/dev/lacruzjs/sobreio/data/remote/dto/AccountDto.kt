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
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
    val balance: Long = 0,
)

/** The columns of `accounts` that the `account_balances` view does not have. */
@Serializable
internal data class AccountStyleDto(val id: String, val color: String? = null, val icon: String? = null)

/** The account columns of the `group_overview` view. */
@Serializable
internal data class AccountsOverviewDto(
    @SerialName("accounts_total") val total: Long? = null,
    @SerialName("in_envelopes_total") val inEnvelopesTotal: Long? = null,
    @SerialName("outside_total") val outsideTotal: Long? = null,
)
