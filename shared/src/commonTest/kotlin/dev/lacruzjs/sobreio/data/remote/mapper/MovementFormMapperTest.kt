package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.TransactionRowDto
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MovementFormMapperTest {

    private fun row(kind: String, amount: Long, voidedAt: String? = null) =
        TransactionRowDto("t1", "g1", "a1", "e1", kind, amount, "2026-11-10", "  ", voidedAt)

    @Test
    fun anExpenseIsReadAsAPositiveAmountAndBlankNotesAreNull() {
        val stored = row("expense", -1_250).toDomain()

        assertEquals(EntryKind.Expense, stored.kind)
        assertEquals(1_250, stored.amountCents)
        assertEquals(LocalDate(2026, 11, 10), stored.date)
        assertNull(stored.notes)
    }

    @Test
    fun adjustmentsAndVoidedRowsAreNotEditable() {
        assertFailsWith<AppError> { row("adjustment", 500).toDomain() }
        assertFailsWith<AppError> { row("expense", -500, voidedAt = "2026-11-11T10:00:00Z").toDomain() }
    }

    @Test
    fun anExpenseIsSentNegativeAndAnIncomePositive() {
        assertEquals(-700, EntryKind.Expense.signed(700))
        assertEquals(700, EntryKind.Income.signed(700))
    }

    @Test
    fun theCoverIsSentAsEnvelopeIdAndAmountPairsOrNotAtAll() {
        assertNull(emptyList<CoverItem>().toCoverJson())
        assertEquals(
            """[{"envelope_id":"e2","amount":3000}]""",
            listOf(CoverItem("e2", 3_000)).toCoverJson().toString(),
        )
    }
}
