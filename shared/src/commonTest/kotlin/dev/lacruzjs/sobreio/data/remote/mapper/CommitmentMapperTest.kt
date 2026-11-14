package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.CommitmentDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentVersionDto
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
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
}
