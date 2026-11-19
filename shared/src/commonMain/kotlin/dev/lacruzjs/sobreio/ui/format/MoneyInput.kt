package dev.lacruzjs.sobreio.ui.format

private const val MAX_INTEGER_DIGITS = 12

/**
 * Reads an amount typed by the user into integer cents. Rules: blank means 0; an optional leading `-`; digits with at
 * most one decimal separator, either `,` or `.`; at most 2 decimals; no thousands separators (so `1.298` is rejected
 * rather than guessed). Returns null when the text is not an amount. Done on the digits, never through floating point.
 */
fun parseMoneyInput(text: String): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    val negative = trimmed.startsWith('-')
    val body = if (negative) trimmed.substring(1) else trimmed
    if (body.count { it == ',' || it == '.' } > 1) return null
    val separator = body.indexOfFirst { it == ',' || it == '.' }
    val units = if (separator < 0) body else body.substring(0, separator)
    val decimals = if (separator < 0) "" else body.substring(separator + 1)
    if (units.isEmpty() && decimals.isEmpty()) return null
    if (!units.all { it in '0'..'9' } || !decimals.all { it in '0'..'9' }) return null
    if (units.length > MAX_INTEGER_DIGITS || decimals.length > 2) return null
    val cents = (units.ifEmpty { "0" }.toLong() * 100) + decimals.padEnd(2, '0').toLong()
    return if (negative) -cents else cents
}

/**
 * Cents as the text a user would type, to prefill an amount field: `12.50` with a plain point, `12` for whole units, and
 * empty for zero or less (nothing to prefill). [parseMoneyInput] reads it back to the same cents.
 */
fun formatMoneyInput(cents: Long): String = when {
    cents <= 0 -> ""
    cents % 100 == 0L -> (cents / 100).toString()
    else -> "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}
