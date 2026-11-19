package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.CalendarRepository
import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.GroupPeriod
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import kotlinx.datetime.LocalDate

/** The group's calendar for tests: today is 14 Nov 2026 in a November period. Set the `*Result` fields to script failures. */
class FakeCalendarRepository : CalendarRepository {

    val calls = mutableListOf<String>()

    var today = LocalDate(2026, 11, 14)
    var period = PeriodRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))
    var previousPeriod: PeriodRange? = PeriodRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))
    var periods = listOf(
        GroupPeriod("p2", LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)),
        GroupPeriod("p1", LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)),
    )

    var todayResult: Result<LocalDate>? = null
    var contextResult: Result<DateContext>? = null

    override suspend fun getToday(groupId: String): Result<LocalDate> {
        calls += "getToday($groupId)"
        return todayResult ?: Result.success(today)
    }

    override suspend fun getCurrentPeriod(groupId: String): Result<PeriodRange> = Result.success(period)

    override suspend fun getDateContext(groupId: String): Result<DateContext> =
        contextResult ?: Result.success(DateContext(today, period, previousPeriod))

    override suspend fun getPeriods(groupId: String, limit: Int): Result<List<GroupPeriod>> = Result.success(periods.take(limit))
}
