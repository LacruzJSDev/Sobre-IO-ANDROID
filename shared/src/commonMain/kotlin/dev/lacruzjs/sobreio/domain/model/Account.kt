package dev.lacruzjs.sobreio.domain.model

/** What an account represents. The database stores it as `account_kind_enum`. */
enum class AccountKind { Cash, Bank, CreditCard, Savings, Investment }

/**
 * A place where the group keeps money. [balanceCents] is computed by the database and may be negative (a credit card
 * with debt); the app never recomputes it. [ownerUserId] is the holder (null for a joint account); [color] is a hex
 * `#RRGGBB` and [icon] a key of the account icon catalogue, both optional; [archivedAt] is set for archived accounts.
 */
data class Account(
    val id: String,
    val groupId: String,
    val name: String,
    val kind: AccountKind,
    val inEnvelopes: Boolean,
    val balanceCents: Long,
    val color: String? = null,
    val icon: String? = null,
    val ownerUserId: String? = null,
    val archivedAt: String? = null,
) {
    val isArchived: Boolean get() = archivedAt != null
}

/**
 * What the user edits of an account besides its balance and scope. The scope (`in_envelopes`) is not here: it only
 * changes through `change_account_scope`.
 */
data class AccountDraft(
    val name: String,
    val kind: AccountKind,
    val ownerUserId: String? = null,
    val color: String? = null,
    val icon: String? = null,
)

/** The figures of the Accounts header, as the database computes them (`group_overview`). */
data class AccountsSummary(
    /** Money in every account. */
    val totalCents: Long,
    /** Money in the accounts that share it into the envelopes. */
    val inEnvelopesCents: Long,
    /** Money in the accounts outside the envelopes (the hidden "Outside envelopes" one). */
    val outsideCents: Long,
)
