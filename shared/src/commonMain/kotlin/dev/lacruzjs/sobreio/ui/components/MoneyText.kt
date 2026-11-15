package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatSignedMoney
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.theme.moneyColorFor

/**
 * A standalone money figure: formatted for the current language and coloured by its [role] (see [MoneyRole]). The sign
 * is always in the text, so colour is never the only cue. Amounts inside a sentence stay plain `formatMoney` text.
 */
@Composable
fun MoneyText(
    amountCents: Long,
    currencyCode: String,
    role: MoneyRole,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val languageTag = Locale.current.toLanguageTag()
    val text = when (role) {
        MoneyRole.Flow -> formatSignedMoney(amountCents, currencyCode, languageTag)
        else -> formatMoney(amountCents, currencyCode, languageTag)
    }
    Text(
        text = text,
        color = moneyColorFor(role, amountCents, LocalMoneyColors.current, MaterialTheme.colorScheme.onSurface),
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier,
    )
}
