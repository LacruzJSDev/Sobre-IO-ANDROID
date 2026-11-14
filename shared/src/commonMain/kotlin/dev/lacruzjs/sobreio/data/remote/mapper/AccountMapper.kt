package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind

internal fun AccountDto.toDomain(): Account = Account(
    id = id,
    groupId = groupId,
    name = name,
    kind = kind.toAccountKind(),
    inEnvelopes = inEnvelopes,
    balanceCents = balance,
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
