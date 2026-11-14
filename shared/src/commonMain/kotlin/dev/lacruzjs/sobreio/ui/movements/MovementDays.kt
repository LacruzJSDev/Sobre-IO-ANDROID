package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Movement
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** How a day header is worded: "Today · 14 Nov", "Yesterday · 13 Nov", or just "Fri 12 Nov". */
enum class DayRelative { Today, Yesterday, Other }

/** The movements of one day, in the order the database gave them. */
data class DaySection(val date: LocalDate, val relative: DayRelative, val movements: List<Movement>)

/** Groups consecutive movements of the same date (the list arrives newest first) and tells which day is today or yesterday. */
fun groupByDay(movements: List<Movement>, today: LocalDate?): List<DaySection> {
    val sections = mutableListOf<DaySection>()
    var date: LocalDate? = null
    var current = mutableListOf<Movement>()
    fun close() {
        val day = date ?: return
        val relative = when {
            today == null -> DayRelative.Other
            day == today -> DayRelative.Today
            day == today.minus(1, DateTimeUnit.DAY) -> DayRelative.Yesterday
            else -> DayRelative.Other
        }
        sections += DaySection(day, relative, current)
    }
    for (movement in movements) {
        if (movement.date != date) {
            close()
            date = movement.date
            current = mutableListOf()
        }
        current += movement
    }
    close()
    return sections
}

/** The choices of the Dates chip. [Custom] opens a range picker. */
enum class DatePreset { ThisPeriod, PreviousPeriod, Last3Months, ThisYear, Custom }

/** First and last day of a preset, or null for [DatePreset.Custom] and for a previous period that does not exist yet. */
fun datePresetRange(preset: DatePreset, context: DateContext): Pair<LocalDate, LocalDate>? = when (preset) {
    DatePreset.ThisPeriod -> context.currentPeriod.start to context.currentPeriod.end
    DatePreset.PreviousPeriod -> context.previousPeriod?.let { it.start to it.end }
    DatePreset.Last3Months -> context.today.minus(3, DateTimeUnit.MONTH).plus(1, DateTimeUnit.DAY) to context.today
    DatePreset.ThisYear -> LocalDate(context.today.year, 1, 1) to context.today
    DatePreset.Custom -> null
}
