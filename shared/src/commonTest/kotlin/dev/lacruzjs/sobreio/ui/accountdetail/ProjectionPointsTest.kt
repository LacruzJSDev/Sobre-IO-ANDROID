package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.ui.components.charts.LineChartPoint
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectionPointsTest {

    private val today = LocalDate(2026, 11, 14)
    private val due = LocalDate(2026, 11, 20)
    private val periodEnd = LocalDate(2026, 11, 30)
    private val projection = listOf(ProjectionPoint(today, 10_000, 0), ProjectionPoint(due, 6_000, -4_000))

    @Test
    fun theLineReachesTheEndOfThePeriodWithTheLastBalance() {
        val points = projectionPoints(AccountDetailUiState(projection = projection, periodEnd = periodEnd))

        assertEquals(
            listOf(
                LineChartPoint(today.toEpochDays(), 10_000),
                LineChartPoint(due.toEpochDays(), 6_000),
                LineChartPoint(periodEnd.toEpochDays(), 6_000),
            ),
            points,
        )
    }

    @Test
    fun nothingIsAddedWhenTheLastPointIsTheEndOrThereIsNoEnd() {
        val endingOnTheEnd = listOf(ProjectionPoint(today, 10_000, 0), ProjectionPoint(periodEnd, 6_000, -4_000))

        assertEquals(2, projectionPoints(AccountDetailUiState(projection = endingOnTheEnd, periodEnd = periodEnd)).size)
        assertEquals(2, projectionPoints(AccountDetailUiState(projection = projection, periodEnd = null)).size)
        assertTrue(projectionPoints(AccountDetailUiState(projection = emptyList(), periodEnd = periodEnd)).isEmpty())
    }
}
