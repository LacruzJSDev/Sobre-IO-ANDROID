package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeOutlookDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopePeriodDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeTransferDto
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EnvelopeDetailMapperTest {

    @Test
    fun mapsARowOfThePeriodsView() {
        val dto = EnvelopePeriodDto("p1", "2026-11-01", "2026-11-30", openingBalance = 6_000, assigned = 40_000, activity = -33_950, closingBalance = 12_050)

        assertEquals(
            EnvelopePeriod("p1", LocalDate(2026, 11, 1), LocalDate(2026, 11, 30), 6_000, 40_000, -33_950, 12_050),
            dto.toDomain(),
        )
    }

    @Test
    fun aPeriodContainsItsFirstAndLastDay() {
        val period = EnvelopePeriodDto("p1", "2026-11-01", "2026-11-30").toDomain()

        assertEquals(true, period.contains(LocalDate(2026, 11, 1)))
        assertEquals(true, period.contains(LocalDate(2026, 11, 30)))
        assertEquals(false, period.contains(LocalDate(2026, 12, 1)))
    }

    @Test
    fun mapsARowOfTheOutlookView() {
        val dto = EnvelopeOutlookDto(balance = 12_050, committed = 20_000, free = -7_950, shortfall = 7_950)

        assertEquals(EnvelopeOutlook(12_050, 20_000, -7_950, 7_950), dto.toDomain())
    }

    @Test
    fun mapsAMoveWithItsOriginAndCommitmentName() {
        val dto = EnvelopeTransferDto("t1", "e0", "e1", 5_000, "commitment", "c1", "2026-11-14T10:30:00.123456+00:00")
        val transfer = dto.toDomain(commitmentName = "Insurance")

        assertEquals(TransferOrigin.Commitment, transfer.origin)
        assertEquals("c1", transfer.commitmentId)
        assertEquals("Insurance", transfer.commitmentName)
        assertEquals(5_000, transfer.amountCents)
        assertEquals(Instant.parse("2026-11-14T10:30:00.123456Z"), transfer.createdAt)
    }

    @Test
    fun everyKnownOriginIsMappedAndAnUnknownOneIsNotManual() {
        assertEquals(TransferOrigin.Manual, "manual".toTransferOrigin())
        assertEquals(TransferOrigin.Target, "target".toTransferOrigin())
        assertEquals(TransferOrigin.Commitment, "commitment".toTransferOrigin())
        assertEquals(TransferOrigin.Release, "release".toTransferOrigin())
        assertEquals(TransferOrigin.Cover, "cover".toTransferOrigin())
        // A value added by a newer database must not be deletable by mistake
        assertEquals(TransferOrigin.Cover, "something-new".toTransferOrigin())
    }
}
