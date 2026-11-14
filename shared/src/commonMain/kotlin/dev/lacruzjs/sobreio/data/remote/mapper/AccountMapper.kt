package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountsOverviewDto
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountsSummary

/** [style] holds the colour and icon, which the balances view does not have; null when the row was not found. */
internal fun AccountDto.toDomain(style: AccountStyleDto? = null): Account = Account(
    id = id,
    groupId = groupId,
    name = name,
    kind = kind.toAccountKind(),
    inEnvelopes = inEnvelopes,
    balanceCents = balance,
    color = style?.color,
    icon = style?.icon,
    ownerUserId = ownerUserId,
    archivedAt = archivedAt,
)

internal fun AccountsOverviewDto?.toSummary(): AccountsSummary = AccountsSummary(
    totalCents = this?.total ?: 0L,
    inEnvelopesCents = this?.inEnvelopesTotal ?: 0L,
    outsideCents = this?.outsideTotal ?: 0L,
)

/** An unknown value (a kind added by a newer database) is shown as a bank account rather than failing the list. */
internal fun String?.toAccountKind(): AccountKind = when (this) {
    "cash" -> AccountKind.Cash
    "credit_card" -> AccountKind.CreditCard
    "savings" -> AccountKind.Savings
    "investment" -> AccountKind.Investment
    else -> AccountKind.Bank
}

/** The value of `account_kind_enum`. */
internal fun AccountKind.toDbValue(): String = when (this) {
    AccountKind.Cash -> "cash"
    AccountKind.Bank -> "bank"
    AccountKind.CreditCard -> "credit_card"
    AccountKind.Savings -> "savings"
    AccountKind.Investment -> "investment"
}
