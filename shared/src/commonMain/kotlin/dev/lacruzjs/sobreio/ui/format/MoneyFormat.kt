package dev.lacruzjs.sobreio.ui.format

import kotlin.math.abs

// Keeps the symbol or code on the same line as the number
private const val NBSP = "\u00A0"

/**
 * Formats integer cents in the style of the app language: `1.298,15 €` in Spanish, `€1,298.15` otherwise
 * (English is the default language). Built by hand because the Spanish locale data skips the thousands separator
 * on four-digit amounts (`1298,15 €`), which breaks column alignment, because going through a floating-point
 * or decimal type is not needed for display, and because there is no common locale API to ask for it. Every group
 * currency is stored in cents (D-01), so there are always two decimals.
 *
 * Only the euro has a symbol; any other currency shows its ISO [currencyCode] instead (`USD 1,298.15`,
 * `1.298,15 USD`, with a no-break space). The UI passes the language tag of the current configuration, for example
 * `es-ES`.
 */
fun formatMoney(amountCents: Long, currencyCode: String, languageTag: String): String {
    val symbol = if (currencyCode == "EUR") "€" else currencyCode
    val spanish = languageTag.substringBefore('-').substringBefore('_').lowercase() == "es"
    // Split before abs() so Long.MIN_VALUE does not overflow
    val units = abs(amountCents / 100)
    val cents = abs(amountCents % 100).toString().padStart(2, '0')
    val sign = if (amountCents < 0) "-" else ""
    val grouped = units.toString().reversed().chunked(3).joinToString(if (spanish) "." else ",").reversed()
    return if (spanish) {
        "$sign$grouped,$cents$NBSP$symbol"
    } else {
        // A code needs a space to stay readable; a symbol hugs the number
        val separator = if (symbol == "€") "" else NBSP
        "$sign$symbol$separator$grouped.$cents"
    }
}
