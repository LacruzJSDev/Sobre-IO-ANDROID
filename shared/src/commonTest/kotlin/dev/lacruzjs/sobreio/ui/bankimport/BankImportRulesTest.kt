package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.bankimport.StatementTable
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.EntryKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BankImportRulesTest {

    private val day = LocalDate(2026, 11, 14)

    private fun row(index: Int, description: String, cents: Long) =
        ImportRow(StatementRow(index, "b1:$index", day, cents, description, null), BankRowStatus.New, BankRowAction.Create)

    // Columns ----------------------------------------------------------------------------------------------------------

    @Test
    fun unambiguousDatesAndAmountsAreNotAsked() {
        val table = StatementTable(
            "nov.csv",
            listOf(listOf("Date", "Concept", "Amount"), listOf("25/11/2026", "Shop", "-42,30"), listOf("13/11/2026", "Fuel", "-1.050,00")),
        )

        val columns = columnsStateFor(table, BlankMapping.copy(dateColumn = 0, descriptionColumn = 1, amountColumn = 2))

        assertFalse(columns.askDateOrder)
        assertFalse(columns.askDecimalMark)
    }

    @Test
    fun withNoColumnChosenBothAreAsked() {
        val table = StatementTable("nov.csv", listOf(listOf("A", "B"), listOf("1", "2")))

        val columns = columnsStateFor(table, BlankMapping)

        assertTrue(columns.askDateOrder)
        assertTrue(columns.askDecimalMark)
    }

    // Preview ----------------------------------------------------------------------------------------------------------

    @Test
    fun aRowTheDatabaseDoesNotAnswerForWasImportedBefore() {
        val statement = StatementRow(0, "b1:0", day, -500, "Shop", null)

        val row = importRowOf(statement, null)

        assertEquals(BankRowStatus.Imported, row.status)
        assertEquals(BankRowAction.Ignore, row.action)
    }

    @Test
    fun aPreviewBringsItsSuggestion() {
        val statement = StatementRow(0, "b1:0", day, -500, "Shop", null)
        val preview = BankRowPreview(0, BankRowStatus.New, BankRowAction.Create, null, null, null, null, "food", emptyList(), emptyList())

        val row = importRowOf(statement, preview)

        assertEquals(BankRowAction.Create, row.action)
        assertEquals("food", row.envelopeId)
        assertEquals("Shop", row.notes)
    }

    // Similar rows and the sheet ---------------------------------------------------------------------------------------

    @Test
    fun similarRowsShareDescriptionAndDirection() {
        val rows = listOf(
            row(0, "MERCADONA", -500),
            row(1, "Mercadona", -700),
            row(2, "Mercadona", 300), // a refund: the other direction
            row(3, "Repsol", -400),
            row(4, "Mercadona", -100).copy(status = BankRowStatus.Imported),
        )

        assertEquals(listOf(1), similarRows(rows, 0))
        assertTrue(similarRows(rows, 9).isEmpty())
    }

    @Test
    fun anEnvelopeChosenForOneRowGoesToTheSimilarOnes() {
        val rows = listOf(row(0, "Shop", -500), row(1, "Shop", -700), row(2, "Fuel", -400))

        val after = applyRowSheet(rows, RowSheetState(0, rows[0].copy(envelopeId = "food"), similarCount = 1))

        assertEquals(listOf("food", "food", null), after.map { it.envelopeId })
    }

    @Test
    fun aChoiceWithoutItsTargetRecordsTheRow() {
        val rows = listOf(row(0, "Shop", -500))

        val confirm = applyRowSheet(rows, RowSheetState(0, rows[0].copy(action = BankRowAction.Confirm), 0))
        assertEquals(BankRowAction.Create, confirm[0].action)

        val link = applyRowSheet(rows, RowSheetState(0, rows[0].copy(action = BankRowAction.Link), 0))
        assertEquals(BankRowAction.Create, link[0].action)

        val transfer = applyRowSheet(rows, RowSheetState(0, rows[0].copy(isTransfer = true), 0))
        assertFalse(transfer[0].isTransfer)
    }

    // Choice -----------------------------------------------------------------------------------------------------------

    @Test
    fun theSignGivesTheKindUnlessItIsATransfer() {
        assertEquals(EntryKind.Expense, row(0, "Shop", -500).toChoice().kind)
        assertEquals(EntryKind.Income, row(0, "Pay", 500).toChoice().kind)

        val transfer = row(0, "To savings", -500).copy(isTransfer = true, envelopeId = "food", otherAccountId = "acc2").toChoice()
        assertEquals(EntryKind.Transfer, transfer.kind)
        assertNull(transfer.envelopeId)
        assertEquals("acc2", transfer.otherAccountId)
    }

    @Test
    fun blankNotesAreNotSent() {
        assertNull(row(0, "Shop", -500).copy(notes = "  ").toChoice().notes)
        assertEquals("Lunch", row(0, "Shop", -500).copy(notes = " Lunch ").toChoice().notes)
    }
}
