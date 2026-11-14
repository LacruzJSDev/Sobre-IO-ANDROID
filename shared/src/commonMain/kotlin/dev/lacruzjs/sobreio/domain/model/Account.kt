package dev.lacruzjs.sobreio.domain.model

/** What an account represents. The database stores it as `account_kind_enum`. */
enum class AccountKind { Cash, Bank, CreditCard, Savings, Investment }

/**
 * A place where the group keeps money. [balanceCents] is computed by the database and may be negative (a credit card
 * with debt); the app never recomputes it.
 */
data class Account(
    val id: String,
    val groupId: String,
    val name: String,
    val kind: AccountKind,
    val inEnvelopes: Boolean,
    val balanceCents: Long,
)
