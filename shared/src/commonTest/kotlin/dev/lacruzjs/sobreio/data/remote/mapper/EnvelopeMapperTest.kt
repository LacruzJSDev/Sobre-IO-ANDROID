package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopesOverviewDto
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
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
}
