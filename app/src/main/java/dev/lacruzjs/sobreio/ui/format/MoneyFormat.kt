package dev.lacruzjs.sobreio.ui.format

import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/**
 * Formats integer cents in the style of the app language: `1.298,15 €` in Spanish, `€1,298.15` otherwise
 * (English is the default language). Built by hand because the Spanish locale data skips the thousands separator
 * on four-digit amounts (`1298,15 €`), which breaks column alignment, and because going through a floating-point
 * or decimal type is not needed for display. Every group currency is stored in cents (D-01), so there are always
 * two decimals. The UI passes the locale of the current configuration.
 */
fun formatMoney(amountCents: Long, currencyCode: String, locale: Locale): String {
    val symbol = Currency.getInstance(currencyCode).getSymbol(locale)
    val spanish = locale.language == "es"
    // Split before abs() so Long.MIN_VALUE does not overflow
    val units = abs(amountCents / 100)
    val cents = abs(amountCents % 100).toString().padStart(2, '0')
    val sign = if (amountCents < 0) "-" else ""
    val grouped = units.toString().reversed().chunked(3).joinToString(if (spanish) "." else ",").reversed()
    return if (spanish) {
        "$sign$grouped,$cents $symbol"
    } else {
        "$sign$symbol$grouped.$cents"
    }
}
