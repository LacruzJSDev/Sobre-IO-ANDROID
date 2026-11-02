package dev.lacruzjs.sobreio.ui.format

import java.util.Currency
import java.util.Locale
import kotlin.math.abs

private val spanish: Locale = Locale.forLanguageTag("es-ES")

/**
 * Formats integer cents in the Spanish style, `1.298,15 €`. Built by hand because the Spanish locale data
 * skips the thousands separator on four-digit amounts (`1298,15 €`), which breaks column alignment.
 * Every group currency is stored in cents (D-01), so there are always two decimals.
 */
fun formatMoney(amountCents: Long, currencyCode: String): String {
    val symbol = Currency.getInstance(currencyCode).getSymbol(spanish)
    // Split before abs() so Long.MIN_VALUE does not overflow
    val units = abs(amountCents / 100)
    val cents = abs(amountCents % 100)
    val sign = if (amountCents < 0) "-" else ""
    val grouped = units.toString().reversed().chunked(3).joinToString(".").reversed()
    return "$sign$grouped,${cents.toString().padStart(2, '0')} $symbol"
}
