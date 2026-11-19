package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/*
 * The group's calendar: its own "today" (in the group's time zone, `group_today`) and its periods (`periods`, created as
 * the calendar reaches them, never in the future; D-08). Read through `CalendarRepository`.
 */

/** First and last day of a period of the group. */
data class PeriodRange(val start: LocalDate, val end: LocalDate)

/** One period the group already has, with its id (to export it, for example). */
data class GroupPeriod(val id: String, val start: LocalDate, val end: LocalDate)

/** Today and the current and previous periods, to turn "This period" and the like into dates. */
data class DateContext(val today: LocalDate, val currentPeriod: PeriodRange, val previousPeriod: PeriodRange?)
