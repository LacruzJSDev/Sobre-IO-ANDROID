package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.CommitmentDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentFundingDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.OccurrenceFactDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentVersionDto
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.OccurrenceStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitmentMapperTest {

    private fun version(id: String, from: String, to: String? = null, kind: String = "expense") = CommitmentVersionDto(
        id = id, commitmentId = "c1", effectiveFrom = from, effectiveTo = to, kind = kind, amount = 5_000,
        accountId = "a1", firstDueDate = from, isRecurring = true, everyN = 2, unit = "week", autoRegister = true,
    )

    @Test
    fun mapsTheFundingStatusAsTheDatabaseGivesIt() {
        val funding = CommitmentFundingDto(
            envelopeId = "e1", nextDueDate = "2027-03-19", cycleAmount = 25_100, cycleStart = "2026-11-01",
            reservedInCycle = 5_020, thisPeriod = 5_020, periodsInCycle = 5, periodsLeft = 4,
        ).toDomain()

        assertEquals("e1", funding.envelopeId)
        assertEquals(LocalDate(2027, 3, 19), funding.nextDueDate)
        assertEquals(LocalDate(2026, 11, 1), funding.cycleStart)
        assertEquals(25_100, funding.cycleAmountCents)
        assertEquals(5_020, funding.reservedInCycleCents)
        assertEquals(5_020, funding.thisPeriodCents)
        assertEquals(5, funding.periodsInCycle)
        assertEquals(4, funding.periodsLeft)
    }

    @Test
    fun mapsAVersionWithItsDatesAndSchedule() {
        val v = version("v1", "2026-01-05", "2026-06-01").toDomain()

        assertEquals(LocalDate(2026, 1, 5), v.effectiveFrom)
        assertEquals(LocalDate(2026, 6, 1), v.effectiveTo)
        assertEquals(CommitmentKind.Expense, v.kind)
        assertEquals(FrequencyUnit.Week, v.unit)
        assertEquals(2, v.everyN)
        assertTrue(v.autoRegister)
        assertNull(version("v2", "2026-06-01").toDomain().effectiveTo)
    }

    @Test
    fun commitmentKeepsItsVersionsNewestFirstAndPicksTheOneInForce() {
        val dto = CommitmentDto("c1", "g1", "Rent")
        val commitment = dto.toDomain(listOf(version("old", "2026-01-01", "2026-06-01"), version("new", "2026-06-01")))

        assertEquals(listOf("new", "old"), commitment.versions.map { it.id })
        assertEquals("old", commitment.currentVersion(LocalDate(2026, 3, 1))?.id)
        assertEquals("new", commitment.currentVersion(LocalDate(2026, 6, 1))?.id)
    }

    @Test
    fun withNoVersionInForceTheNewestIsCurrent() {
        val commitment = CommitmentDto("c1", "g1", "Rent").toDomain(listOf(version("only", "2026-01-01", "2026-02-01")))

        assertEquals("only", commitment.currentVersion(LocalDate(2026, 11, 1))?.id)
    }

    @Test
    fun kindsAndUnitsRoundTripToTheirDatabaseValues() {
        listOf("expense", "income", "transfer", "group_transfer_out", "group_transfer_in").forEach {
            assertEquals(it, it.toCommitmentKind().toDbValue())
        }
        listOf("day", "week", "month", "year").forEach { assertEquals(it, it.toFrequencyUnit().toDbValue()) }
    }

    @Test
    fun aRegisteredOccurrenceTakesTheAmountAndDateOfItsTransaction() {
        val o = CommitmentOccurrenceDto(
            dueDate = "2026-11-05", status = "materialized", transactionId = "t1",
            transactions = OccurrenceFactDto(amount = -70_050, date = "2026-11-07"),
        ).toDomain()

        assertEquals(LocalDate(2026, 11, 5), o.dueDate)
        assertEquals(OccurrenceStatus.Materialized, o.status)
        assertEquals(-70_050, o.amountCents)
        assertEquals(LocalDate(2026, 11, 7), o.date)
        assertNull(o.fromAccountId)
    }

    @Test
    fun aTransferOccurrenceKeepsItsSourceAccount() {
        val o = CommitmentOccurrenceDto(
            dueDate = "2026-11-05", status = "materialized", transferId = "x1",
            transfer = OccurrenceFactDto(amount = 5_000, date = "2026-11-05", fromAccountId = "a1"),
        ).toDomain()

        assertEquals(5_000, o.amountCents)
        assertEquals("a1", o.fromAccountId)
    }

    @Test
    fun aSkippedOccurrenceHasNoMovement() {
        val o = CommitmentOccurrenceDto(dueDate = "2026-10-05", status = "skipped").toDomain()

        assertEquals(OccurrenceStatus.Skipped, o.status)
        assertNull(o.amountCents)
        assertNull(o.date)
    }
}
