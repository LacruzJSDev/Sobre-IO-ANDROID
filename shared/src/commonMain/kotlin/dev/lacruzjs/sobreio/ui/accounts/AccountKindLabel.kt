package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_kind_bank
import dev.lacruzjs.sobreio.generated.resources.account_kind_cash
import dev.lacruzjs.sobreio.generated.resources.account_kind_credit_card
import dev.lacruzjs.sobreio.generated.resources.account_kind_investment
import dev.lacruzjs.sobreio.generated.resources.account_kind_savings
import org.jetbrains.compose.resources.StringResource

internal fun AccountKind.labelRes(): StringResource = when (this) {
    AccountKind.Cash -> Res.string.account_kind_cash
    AccountKind.Bank -> Res.string.account_kind_bank
    AccountKind.CreditCard -> Res.string.account_kind_credit_card
    AccountKind.Savings -> Res.string.account_kind_savings
    AccountKind.Investment -> Res.string.account_kind_investment
}
