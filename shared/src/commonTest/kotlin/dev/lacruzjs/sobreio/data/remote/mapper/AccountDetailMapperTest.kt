package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.MovementDto
import dev.lacruzjs.sobreio.data.remote.dto.UpcomingDto
import dev.lacruzjs.sobreio.domain.model.MovementKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountDetailMapperTest {

    private fun due(kind: String, from: String?, to: String? = null) =
        UpcomingDto("c1", "Rent", kind, "2026-11-20", 50_000, from, to)

    @Test
    fun anExpenseLeavesTheAccountAndAnIncomeEntersIt() {
        assertEquals(-50_000, due("expense", "a1").toUpcomingFor("a1")?.amountCents)
        assertEquals(50_000, due("income", "a1").toUpcomingFor("a1")?.amountCents)
        assertEquals(50_000, due("group_transfer_in", "a1").toUpcomingFor("a1")?.amountCents)
        assertEquals(-50_000, due("group_transfer_out", "a1").toUpcomingFor("a1")?.amountCents)
    }

    @Test
    fun aTransferLeavesItsSourceAndEntersItsDestination() {
        assertEquals(-50_000, due("transfer", "a1", "a2").toUpcomingFor("a1")?.amountCents)
        assertEquals(50_000, due("transfer", "a1", "a2").toUpcomingFor("a2")?.amountCents)
    }

    @Test
    fun anItemOfAnotherAccountIsLeftOut() {
        assertNull(due("expense", "a3").toUpcomingFor("a1"))
        assertNull(due("transfer", "a3", "a4").toUpcomingFor("a1"))
    }

    @Test
    fun theUpcomingItemKeepsItsNameAndDate() {
        val item = due("expense", "a1").toUpcomingFor("a1")
        assertEquals("Rent", item?.name)
        assertEquals(LocalDate(2026, 11, 20), item?.dueDate)
    }

    @Test
    fun movementKindsMapAndUnknownOnesReadAsAdjustments() {
        assertEquals(MovementKind.TransferIn, "transfer_in".toMovementKind())
        assertEquals(MovementKind.GroupTransferOut, "group_transfer_out".toMovementKind())
        assertEquals(MovementKind.Adjustment, "something_new".toMovementKind())
    }

    @Test
    fun aBlankNoteIsNoNote() {
        val dto = MovementDto("m", "expense", -100, "2026-11-13", notes = "  ")
        assertNull(dto.toDomain().notes)
    }
}
