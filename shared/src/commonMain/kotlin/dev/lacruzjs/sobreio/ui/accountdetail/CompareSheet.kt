package dev.lacruzjs.sobreio.ui.accountdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_adjust_balance
import dev.lacruzjs.sobreio.generated.resources.action_compare_bank
import dev.lacruzjs.sobreio.generated.resources.compare_app_balance
import dev.lacruzjs.sobreio.generated.resources.compare_difference
import dev.lacruzjs.sobreio.generated.resources.compare_matches
import dev.lacruzjs.sobreio.generated.resources.field_bank_balance
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatSignedMoney
import org.jetbrains.compose.resources.stringResource

/**
 * Short one-step sheet: the balance the user's bank shows and, as they type, how it compares with the app's (the database
 * answers after a short pause). A difference offers one button that records it as an adjustment.
 */
@Composable
internal fun CompareSheet(
    compare: CompareState,
    appBalanceCents: Long,
    currencyCode: String,
    onEvent: (AccountDetailEvent) -> Unit,
) {
    val languageTag = Locale.current.toLanguageTag()
    FlatBottomSheet(
        title = stringResource(Res.string.action_compare_bank),
        onDismiss = { onEvent(AccountDetailEvent.CompareDismissed) },
    ) {
        FormTile(label = stringResource(Res.string.compare_app_balance)) {
            FigureText(appBalanceCents, currencyCode)
        }
        AmountField(
            value = compare.bankInput,
            onValueChange = { onEvent(AccountDetailEvent.BankBalanceChanged(it)) },
            label = stringResource(Res.string.field_bank_balance),
            errorRes = compare.inputError,
            enabled = !compare.isAdjusting,
            allowNegative = true,
        )
        // Reserved from the start, so the sheet does not jump when the answer arrives
        val result = compare.result
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
            when {
                compare.isComparing -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                result == null -> Unit
                result.differenceCents == 0L -> Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(text = stringResource(Res.string.compare_matches), style = MaterialTheme.typography.titleMedium)
                }
                else -> FormTile(label = stringResource(Res.string.compare_difference)) {
                    FigureText(result.differenceCents, currencyCode, MoneyRole.Flow)
                }
            }
        }
        if (result != null && result.differenceCents != 0L) {
            PrimaryButton(
                text = stringResource(Res.string.action_adjust_balance),
                onClick = { onEvent(AccountDetailEvent.AdjustClicked) },
                isLoading = compare.isAdjusting,
            )
        }
        ErrorText(compare.error, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
/** A figure of the sheet; the difference keeps its sign (Flow), the app balance is a Balance. */
private fun FigureText(amountCents: Long, currencyCode: String, role: MoneyRole = MoneyRole.Balance) {
    MoneyText(
        amountCents = amountCents,
        currencyCode = currencyCode,
        role = role,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(end = 8.dp),
        maxLines = 1,
    )
}
