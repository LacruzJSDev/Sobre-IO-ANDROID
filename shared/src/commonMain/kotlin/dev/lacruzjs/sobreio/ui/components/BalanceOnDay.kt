package dev.lacruzjs.sobreio.ui.components

import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.balance_on_day_choose
import dev.lacruzjs.sobreio.generated.resources.balance_on_day_label
import dev.lacruzjs.sobreio.generated.resources.balance_on_day_result
import dev.lacruzjs.sobreio.ui.format.formatDate
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * "How much was there on…?": a day picker (up to [today]) and, once the database has answered
 * (`account_balance_at` / `envelope_balance_at`), the balance at the end of that day. Used by the account and envelope
 * details; the ViewModel asks the database, nothing is summed here.
 */
@Composable
fun BalanceOnDay(
    day: LocalDate?,
    balanceCents: Long?,
    currencyCode: String,
    today: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val languageTag = Locale.current.toLanguageTag()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DateFormTile(
            label = stringResource(Res.string.balance_on_day_label),
            date = day,
            onDateSelected = onDaySelected,
            emptyText = stringResource(Res.string.balance_on_day_choose),
            maxDate = today,
        )
        if (day != null && balanceCents != null) {
            Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.balance_on_day_result, formatDate(day, languageTag)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                MoneyText(
                    amountCents = balanceCents,
                    currencyCode = currencyCode,
                    role = MoneyRole.Balance,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
