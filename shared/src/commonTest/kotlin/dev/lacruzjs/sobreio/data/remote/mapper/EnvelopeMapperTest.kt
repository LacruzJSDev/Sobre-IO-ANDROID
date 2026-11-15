package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeProgressDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopesOverviewDto
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeProgress
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvelopeMapperTest {

    @Test
    fun mapsARowOfTheBalancesView() {
        val dto = EnvelopeDto("e1", "g1", "Food", "spending", monthlyTarget = 20000, balance = -500)

        assertEquals(Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 20000, -500), dto.toDomain())
    }

    @Test
    fun joinsTheColourIconHolderAndArchiveDateOfTheRow() {
        val dto = EnvelopeDto("e1", "g1", "Food", "spending", ownerUserId = "u2", archivedAt = "2026-11-01T10:00:00Z")
        val envelope = dto.toDomain(EnvelopeStyleDto("e1", color = "#4F7A5A", icon = "groceries"))

        assertEquals("#4F7A5A", envelope?.color)
        assertEquals("groceries", envelope?.icon)
        assertEquals("u2", envelope?.ownerUserId)
        assertEquals(true, envelope?.isArchived)
    }

    @Test
    fun theSystemEnvelopeIsDetectedByItsKind() {
        val dto = EnvelopeDto("e0", "g1", "Sin asignar", "system", balance = 1000)

        assertEquals(EnvelopeKind.System, dto.toDomain()?.kind)
    }

    @Test
    fun externalAndUnknownKindsAreFilteredOut() {
        assertNull(EnvelopeDto("e2", "g1", "Fuera de sobres", "external").toDomain())
        assertNull(EnvelopeDto("e3", "g1", "X", "future_kind").toDomain())
    }

    @Test
    fun kindsRoundTripThroughTheirDatabaseValue() {
        EnvelopeKind.entries.forEach { assertEquals(it, it.toDbValue().toEnvelopeKind()) }
        assertEquals("saving", EnvelopeKind.Saving.toDbValue())
    }

    @Test
    fun summaryComesFromTheOverviewAndDefaultsToZero() {
        assertEquals(EnvelopeSummary(900, 100), EnvelopesOverviewDto(900, 100).toSummary())
        assertEquals(EnvelopeSummary(0, 0), (null as EnvelopesOverviewDto?).toSummary())
    }

    @Test
    fun theTypeComesFromTheEnvelopesTable() {
        val dto = EnvelopeDto("e1", "g1", "Utilities", "spending")

        assertEquals(EnvelopePurpose.Bills, dto.toDomain(EnvelopeStyleDto("e1", purpose = "bills"))?.purpose)
        assertNull(dto.toDomain(EnvelopeStyleDto("e1"))?.purpose)
        assertNull(dto.toDomain(EnvelopeStyleDto("e1", purpose = "from_the_future"))?.purpose)
    }

    @Test
    fun purposesRoundTripThroughTheirDatabaseValue() {
        EnvelopePurpose.entries.forEach { assertEquals(it, it.toDbValue().toEnvelopePurpose()) }
        assertEquals("bills", EnvelopePurpose.Bills.toDbValue())
        assertEquals("monthly", EnvelopePurpose.Monthly.toDbValue())
        assertNull(null.toEnvelopePurpose())
    }

    @Test
    fun everyProgressStateIsMappedAndAnUnknownOneHasNoBar() {
        val expected = mapOf(
            "remaining" to ProgressState.Remaining,
            "reserving" to ProgressState.Reserving,
            "paid" to ProgressState.Paid,
            "contributing" to ProgressState.Contributing,
            "overspent" to ProgressState.Overspent,
            "none" to ProgressState.None,
            "something_new" to ProgressState.None,
        )
        expected.forEach { (text, state) -> assertEquals(state, text.toProgressState()) }
    }

    @Test
    fun aProgressRowOfTheFunctionIsDecodedWithItsDates() {
        val dto = Json.decodeFromString<EnvelopeProgressDto>(
            """{"envelope_id":"e1","purpose":"bills","state":"reserving","current":3000,"goal":9000,""" +
                """"next_due":"2026-12-05","paid_on":null,"pending":2}""",
        )

        assertEquals(
            EnvelopeProgress("e1", EnvelopePurpose.Bills, ProgressState.Reserving, 3000, 9000, LocalDate(2026, 12, 5), null, 2),
            dto.toDomain(),
        )
    }

    @Test
    fun theBarFillIsOnlyAClampOfTheTwoFigures() {
        fun fraction(state: ProgressState, current: Long, goal: Long) =
            EnvelopeProgress("e", null, state, current, goal).fraction

        assertEquals(0.25f, fraction(ProgressState.Remaining, 250, 1000))
        assertEquals(1f, fraction(ProgressState.Contributing, 7000, 5000))
        assertEquals(0f, fraction(ProgressState.Remaining, -50, 1000))
        assertEquals(0f, fraction(ProgressState.Reserving, 100, 0))
        assertEquals(1f, fraction(ProgressState.Paid, 0, 0))
        assertEquals(1f, fraction(ProgressState.Overspent, -300, 0))
    }
}
