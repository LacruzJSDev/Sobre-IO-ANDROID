package dev.lacruzjs.sobreio.ui.format

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
    val date = instant.toLocalDateTime(timeZone)
    val spanish = languageTag.substringBefore('-').substringBefore('_').lowercase() == "es"
    return if (spanish) {
        "${date.day} ${SpanishMonths[date.month.ordinal]} ${date.year}"
    } else {
        "${EnglishMonths[date.month.ordinal]} ${date.day}, ${date.year}"
    }
}
