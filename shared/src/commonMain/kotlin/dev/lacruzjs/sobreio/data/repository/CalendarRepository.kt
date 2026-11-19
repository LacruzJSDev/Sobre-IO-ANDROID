package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.GroupPeriod
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import kotlinx.datetime.LocalDate

/**
 * The group's calendar as the database keeps it: the group's own today and its periods. Every screen that needs a date
 * reads it here, never from the device's clock. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface CalendarRepository {

    /** The group's today, in the group's time zone (`group_today`). */
    suspend fun getToday(groupId: String): Result<LocalDate>

    /** First and last day of the group's current period (`current_period`). */
    suspend fun getCurrentPeriod(groupId: String): Result<PeriodRange>

    /** Today with the current and the previous period, for date presets ("This period", "Previous period"). */
    suspend fun getDateContext(groupId: String): Result<DateContext>

    /** The group's periods up to today's, newest first, at most [limit]. */
    suspend fun getPeriods(groupId: String, limit: Int = 12): Result<List<GroupPeriod>>
}
