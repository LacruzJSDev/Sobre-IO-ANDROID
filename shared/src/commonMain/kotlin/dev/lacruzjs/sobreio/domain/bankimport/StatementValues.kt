package dev.lacruzjs.sobreio.domain.bankimport

import kotlinx.datetime.LocalDate

/** Order of day, month and year in the statement's dates. */
enum class DateOrder { DayMonthYear, MonthDayYear, YearMonthDay }

/** The character before the cents: `1.234,56` (comma) or `1,234.56` (dot). */
enum class DecimalMark { Comma, Dot }

private val dateParts = Regex("""^\s*(\d{1,4})[/.\-](\d{1,2})[/.\-](\d{1,4})""")

/** A date of the statement in [order], with `/`, `-` or `.` between parts and two or four digit years; null if it is not one. */
fun parseStatementDate(text: String, order: DateOrder): LocalDate? {
    val match = dateParts.find(text) ?: return null
    val (a, b, c) = match.destructured
    val (year, month, day) = when (order) {
        DateOrder.DayMonthYear -> Triple(c, b, a)
        DateOrder.MonthDayYear -> Triple(c, a, b)
        DateOrder.YearMonthDay -> Triple(a, b, c)
    }
    val y = year.toIntOrNull()?.let { if (year.length <= 2) 2000 + it else it } ?: return null
    if (year.length == 3) return null
    return runCatching { LocalDate(y, month.toInt(), day.toInt()) }.getOrNull()
}

/**
 * The order the dates of a column are written in: year first when a value starts with four digits; month
 * first only when some second part is above 12 and no first part is; otherwise day first (Spain).
 * [ambiguous] is true when every value fits both day-first and month-first, so the user is asked.
 */
data class DateOrderGuess(val order: DateOrder, val ambiguous: Boolean)

fun guessDateOrder(values: List<String>): DateOrderGuess {
    val parts = values.mapNotNull { dateParts.find(it)?.destructured?.toList() }
    if (parts.isEmpty()) return DateOrderGuess(DateOrder.DayMonthYear, ambiguous = false)
    if (parts.any { it[0].length == 4 }) return DateOrderGuess(DateOrder.YearMonthDay, ambiguous = false)
    val firstAbove12 = parts.any { (it[0].toIntOrNull() ?: 0) > 12 }
    val secondAbove12 = parts.any { (it[1].toIntOrNull() ?: 0) > 12 }
    return when {
        firstAbove12 -> DateOrderGuess(DateOrder.DayMonthYear, ambiguous = false)
        secondAbove12 -> DateOrderGuess(DateOrder.MonthDayYear, ambiguous = false)
        else -> DateOrderGuess(DateOrder.DayMonthYear, ambiguous = true)
    }
}

private val amountNoise = Regex("""[\s €$£]|EUR|USD|GBP""", RegexOption.IGNORE_CASE)

/**
 * An amount of the statement in cents, or null. Accepts a leading or trailing sign (`-`, `+`, `−`),
 * parentheses for negatives, thousands marks, currency symbols and codes, and one or two decimals.
 */
fun parseStatementAmount(text: String, mark: DecimalMark): Long? {
    var s = text.replace(amountNoise, "").replace('−', '-')
    if (s.isEmpty()) return null
    var negative = false
    if (s.startsWith("(") && s.endsWith(")")) {
        negative = true
        s = s.substring(1, s.length - 1)
    }
    when {
        s.startsWith("-") -> { negative = !negative; s = s.substring(1) }
        s.startsWith("+") -> s = s.substring(1)
        s.endsWith("-") -> { negative = !negative; s = s.dropLast(1) }
        s.endsWith("+") -> s = s.dropLast(1)
    }
    val decimal = if (mark == DecimalMark.Comma) ',' else '.'
    val thousands = if (mark == DecimalMark.Comma) '.' else ','
    s = s.replace(thousands.toString(), "")
    if (s.isEmpty() || s.count { it == decimal } > 1 || !s.all { it.isDigit() || it == decimal }) return null
    val whole = s.substringBefore(decimal)
    val fraction = if (decimal in s) s.substringAfter(decimal) else ""
    if (fraction.length > 2 || (whole.isEmpty() && fraction.isEmpty())) return null
    val cents = (whole.ifEmpty { "0" }.toLongOrNull() ?: return null) * 100 + fraction.padEnd(2, '0').ifEmpty { "00" }.toLong()
    return if (negative) -cents else cents
}

/**
 * The decimal mark of a column of amounts: the last of `,` and `.` in a value is the decimal one when it has
 * one or two digits after it. With no such value it is a comma (Spain); [ambiguous] then says so.
 */
data class DecimalMarkGuess(val mark: DecimalMark, val ambiguous: Boolean)

fun guessDecimalMark(values: List<String>): DecimalMarkGuess {
    var comma = 0
    var dot = 0
    for (value in values) {
        val v = value.trim()
        val last = maxOf(v.lastIndexOf(','), v.lastIndexOf('.'))
        if (last < 0) continue
        val digitsAfter = v.substring(last + 1).takeWhile { it.isDigit() }.length
        if (digitsAfter in 1..2) {
            if (v[last] == ',') comma++ else dot++
        } else if (digitsAfter == 3) {
            // "1.234" or "1,234": a thousands mark, so the decimal one is the other
            if (v[last] == '.') comma++ else dot++
        }
    }
    return when {
        comma == 0 && dot == 0 -> DecimalMarkGuess(DecimalMark.Comma, ambiguous = true)
        dot > comma -> DecimalMarkGuess(DecimalMark.Dot, ambiguous = false)
        else -> DecimalMarkGuess(DecimalMark.Comma, ambiguous = false)
    }
}
