package dev.lacruzjs.sobreio.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val EnglishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val SpanishMonths = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sept", "oct", "nov", "dic")

/**
 * A day written for the language of the app: `Nov 13, 2026` in English (the default) and `13 nov 2026` in Spanish. Built
 * by hand for the same reason as [formatMoney]: there is no common locale API. [languageTag] is the one of the current
 * configuration, for example `es-ES`; [timeZone] is where the day is read (the device's by default).
 */
fun formatDate(instant: Instant, languageTag: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    return formatDate(instant.toLocalDateTime(timeZone).date, languageTag)
}

/** The same as [formatDate] for a day that is already a calendar date (a movement's or a period's). */
fun formatDate(date: LocalDate, languageTag: String): String =
    if (isSpanish(languageTag)) {
        "${date.day} ${SpanishMonths[date.month.ordinal]} ${date.year}"
    } else {
        "${EnglishMonths[date.month.ordinal]} ${date.day}, ${date.year}"
    }

/** Day and month without the year, for axes and tooltips: `Nov 14` in English, `14 nov` in Spanish. */
fun formatDayMonth(date: LocalDate, languageTag: String): String =
    if (isSpanish(languageTag)) {
        "${date.day} ${SpanishMonths[date.month.ordinal]}"
    } else {
        "${EnglishMonths[date.month.ordinal]} ${date.day}"
    }

/** Three-letter month of [date]: `Nov` in English, `nov` in Spanish. */
fun formatMonthShort(date: LocalDate, languageTag: String): String =
    if (isSpanish(languageTag)) SpanishMonths[date.month.ordinal] else EnglishMonths[date.month.ordinal]

private fun isSpanish(languageTag: String) = languageTag.substringBefore('-').substringBefore('_').lowercase() == "es"
