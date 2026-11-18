package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.ExistingCandidate
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeBankFormatStore
import dev.lacruzjs.sobreio.fake.FakeBankImportRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import dev.lacruzjs.sobreio.ui.bankimport.picker.PickResult
import dev.lacruzjs.sobreio.ui.bankimport.picker.PickedFile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BankImportViewModelTest : MainDispatcherTest() {

    private val bank = Account("a1", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val savings = Account("a2", "g1", "Savings", AccountKind.Savings, true, 0)
    private val unassigned = Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 0)
    private val food = Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 0, 0)

    private val groups = FakeGroupRepository(listOf(Group("g1", "Home", "EUR", GroupRole.Member)))
    private val accounts = FakeAccountRepository(listOf(bank, savings))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food))
    private val detail = FakeAccountDetailRepository()
    private val imports = FakeBankImportRepository()
    private val formats = FakeBankFormatStore()

    private val viewModel by lazy { BankImportViewModel("g1", "a1", groups, accounts, envelopes, detail, imports, formats) }

    private val statement = """
        F.Operativa;Concepto;Importe;Saldo
        14/11/2026;MERCADONA;-42,30;1.208,15
        13/11/2026;REPSOL;-50,00;1.250,45
        12/11/2026;REPSOL;-30,00;1.300,45
        10/11/2026;NOMINA;1.500,00;1.330,45
    """.trimIndent()

    private fun settle() = testDispatcher.scheduler.advanceUntilIdle()

    private fun state() = viewModel.state.value

    private fun pick(text: String = statement) {
        viewModel.onEvent(BankImportEvent.FilePicked(PickResult.Picked(PickedFile("nov.csv", text.encodeToByteArray()))))
        settle()
    }

    /** Picks the statement and accepts the guessed columns: the review is on screen. */
    private fun toReview() {
        settle()
        pick()
        viewModel.onEvent(BankImportEvent.ColumnsContinueClicked)
        settle()
    }

    private fun indexOf(description: String, cents: Long) =
        state().rows.indexOfFirst { it.row.description == description && it.row.amountCents == cents }

    @Test
    fun opensOnTheSourceStepWithTheEnvelopesAndOtherAccounts() {
        settle()
        assertEquals(ImportStep.Source, state().step)
        assertTrue(state().canImport)
        assertEquals(listOf(food), state().envelopes)
        assertEquals(listOf(savings), state().otherAccounts)
    }

    @Test
    fun anArchivedAccountCannotImport() {
        accounts.accounts = listOf(bank.copy(archivedAt = "2026-11-01T00:00:00Z"), savings)
        settle()
        assertFalse(state().canImport)
    }

    @Test
    fun aNewFormatAsksForColumnsAndIsRememberedForTheAccount() {
        settle()
        pick()
        assertEquals(ImportStep.Columns, state().step)
        val mapping = assertNotNull(state().columns).mapping
        assertEquals(0, mapping.dateColumn)
        assertEquals(2, mapping.amountColumn)

        viewModel.onEvent(BankImportEvent.ColumnsContinueClicked)
        settle()
        assertEquals(ImportStep.Review, state().step)
        assertEquals(4, state().rows.size)
        assertEquals(120_815L, state().closingBalanceCents)
        assertEquals(1, formats.saved.size)
    }

    @Test
    fun aRememberedFormatGoesStraightToTheReview() {
        toReview()
        viewModel.onEvent(BankImportEvent.CloseClicked) // warns: the review has rows
        viewModel.onEvent(BankImportEvent.DiscardConfirmed)
        settle()

        val second = BankImportViewModel("g1", "a1", groups, accounts, envelopes, detail, imports, formats)
        settle()
        second.onEvent(BankImportEvent.FilePicked(PickResult.Picked(PickedFile("nov.csv", statement.encodeToByteArray()))))
        settle()
        assertEquals(ImportStep.Review, second.state.value.step)
        assertFalse(second.state.value.cameThroughColumns)
    }

    @Test
    fun anIncompleteMappingIsNotAccepted() {
        settle()
        pick()
        viewModel.onEvent(BankImportEvent.ColumnChosen(ColumnTile.Date, -1))
        viewModel.onEvent(BankImportEvent.ColumnsContinueClicked)
        settle()
        assertEquals(ImportStep.Columns, state().step)
        assertNotNull(state().columns?.error)
    }

    @Test
    fun aFileTooLargeOrUnreadableStaysOnTheSource() {
        settle()
        viewModel.onEvent(BankImportEvent.FilePicked(PickResult.TooLarge))
        assertNotNull(state().sourceError)
        pick("just one line")
        assertEquals(ImportStep.Source, state().step)
        assertNotNull(state().sourceError)
    }

    @Test
    fun theReviewFollowsTheDatabaseSuggestions() {
        imports.previews = mapOf(
            0 to FakeBankImportRepository.defaultPreview(0).copy(
                suggestedAction = BankRowAction.Link, matchSource = MovementSource.Transaction, matchId = "t1",
                existing = listOf(ExistingCandidate(MovementSource.Transaction, "t1", LocalDate(2026, 11, 14), -4_230, "Mercadona", MovementKind.Expense)),
            ),
            3 to FakeBankImportRepository.defaultPreview(3).copy(status = BankRowStatus.Imported, suggestedAction = BankRowAction.Ignore),
        )
        toReview()
        assertEquals(1, state().section(ImportSection.Recorded).size)
        assertEquals(2, state().section(ImportSection.New).size)
        assertEquals(1, state().section(ImportSection.AlreadyImported).size)
        // The imported row is not sent and does not count
        assertEquals(3, state().recordCount)
        assertEquals(3, state().sentCount)
    }

    @Test
    fun anEnvelopeChosenForOneRowGoesToTheRowsWithTheSameDescription() {
        toReview()
        val first = indexOf("REPSOL", -5_000)
        viewModel.onEvent(BankImportEvent.RowClicked(first))
        assertEquals(1, state().sheet?.similarCount)
        viewModel.onEvent(BankImportEvent.SheetEnvelopeChosen("e1"))
        viewModel.onEvent(BankImportEvent.SheetDoneClicked)

        assertNull(state().sheet)
        assertEquals("e1", state().rows[indexOf("REPSOL", -5_000)].envelopeId)
        assertEquals("e1", state().rows[indexOf("REPSOL", -3_000)].envelopeId)
        assertNull(state().rows[indexOf("MERCADONA", -4_230)].envelopeId)
    }

    @Test
    fun aTransferWithoutTheOtherAccountStaysAnExpense() {
        toReview()
        val row = indexOf("MERCADONA", -4_230)
        viewModel.onEvent(BankImportEvent.RowClicked(row))
        viewModel.onEvent(BankImportEvent.SheetTransferChosen(true))
        viewModel.onEvent(BankImportEvent.SheetDoneClicked)
        assertFalse(state().rows[row].isTransfer)

        viewModel.onEvent(BankImportEvent.RowClicked(row))
        viewModel.onEvent(BankImportEvent.SheetTransferChosen(true))
        viewModel.onEvent(BankImportEvent.SheetOtherAccountChosen("a2"))
        viewModel.onEvent(BankImportEvent.SheetDoneClicked)
        assertTrue(state().rows[row].isTransfer)
        assertEquals("a2", state().rows[row].otherAccountId)
    }

    @Test
    fun ignoringARowCanBeUndone() {
        toReview()
        val row = indexOf("NOMINA", 150_000)
        viewModel.onEvent(BankImportEvent.RowIgnoreSwiped(row))
        assertEquals(BankRowAction.Ignore, state().rows[row].action)
        assertNotNull(state().undoMessage)
        assertEquals(3, state().recordCount)

        viewModel.onEvent(BankImportEvent.UndoClicked)
        settle()
        assertEquals(BankRowAction.Create, state().rows[row].action)
    }

    @Test
    fun backFromTheReviewWarnsAndDiscardingReturnsToTheColumns() {
        toReview()
        viewModel.onEvent(BankImportEvent.BackPressed)
        assertEquals(DiscardTarget.PreviousStep, state().showDiscardWarning)
        viewModel.onEvent(BankImportEvent.DiscardCancelled)
        assertEquals(ImportStep.Review, state().step)

        viewModel.onEvent(BankImportEvent.BackPressed)
        viewModel.onEvent(BankImportEvent.DiscardConfirmed)
        assertEquals(ImportStep.Columns, state().step)
        assertTrue(state().rows.isEmpty())

        viewModel.onEvent(BankImportEvent.BackPressed)
        assertEquals(ImportStep.Source, state().step)
        viewModel.onEvent(BankImportEvent.BackPressed)
        assertTrue(state().close)
    }

    @Test
    fun importingSendsTheDecisionsWithTheTagAndComparesTheBalance() {
        toReview()
        viewModel.onEvent(BankImportEvent.RowIgnoreSwiped(indexOf("NOMINA", 150_000)))
        viewModel.onEvent(BankImportEvent.ImportClicked("Imported"))
        settle()

        val sent = imports.applied.single()
        assertEquals(4, sent.size)
        assertEquals(listOf("Imported"), imports.appliedTags)
        assertEquals(BankRowAction.Ignore, sent.single { it.description == "NOMINA" }.action)
        assertEquals("MERCADONA", sent.single { it.amountCents == -4_230L }.notes)
        assertEquals(ImportStep.Result, state().step)
        assertEquals(3, state().result?.created)
        // The CSV's closing balance is compared with the app's
        assertEquals(120_815L, state().comparison?.bankBalanceCents)
        assertTrue("compareWithBank(120815)" in detail.calls)

        // Back from the result closes with no warning
        viewModel.onEvent(BankImportEvent.BackPressed)
        assertNull(state().showDiscardWarning)
        assertTrue(state().close)
    }

    @Test
    fun theImportCanBeUndoneFromTheResult() {
        toReview()
        viewModel.onEvent(BankImportEvent.ImportClicked("Imported"))
        settle()
        viewModel.onEvent(BankImportEvent.UndoImportClicked)
        settle()
        assertEquals(listOf("imp1"), imports.undone)
        assertTrue(state().undone)
    }

    @Test
    fun seeMovementsOpensTheTaggedList() {
        toReview()
        viewModel.onEvent(BankImportEvent.ImportClicked("Imported"))
        settle()
        viewModel.onEvent(BankImportEvent.SeeMovementsClicked)
        assertTrue(state().openMovements)
        assertEquals("tag1", state().result?.tagId)
        viewModel.onEvent(BankImportEvent.NavigationHandled)
        assertFalse(state().openMovements)
    }
}
