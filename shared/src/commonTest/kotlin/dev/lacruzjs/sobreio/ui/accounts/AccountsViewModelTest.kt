package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_deleted
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountsViewModelTest : MainDispatcherTest() {

    private val bank = Account("1", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val repository = FakeAccountRepository(listOf(bank, cash)).also { it.total = 105_000 }
    private val viewModel by lazy { AccountsViewModel("g1", repository) }

    private fun loaded(canEdit: Boolean = true): AccountsViewModel = viewModel.also {
        it.onEvent(AccountsEvent.GroupLoaded("USD", canEdit))
        it.onEvent(AccountsEvent.Refresh)
    }

    @Test
    fun refreshLoadsTheAccountsAndTheTotalFromTheDatabase() {
        loaded()

        val state = viewModel.state.value
        assertEquals(listOf(bank, cash), state.accounts)
        assertEquals(105_000, state.totalCents)
        assertEquals("USD", state.currencyCode)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun failedLoadShowsTheError() {
        repository.accountsResult = Result.failure(AppError.Network)
        loaded()

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun createSendsTrimmedNameKindAndOpeningBalanceThenReloads() {
        loaded()
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.NameChanged("  Visa  "))
        viewModel.onEvent(AccountsEvent.KindSelected(AccountKind.CreditCard))
        viewModel.onEvent(AccountsEvent.OpeningBalanceChanged("-250,5"))
        repository.calls.clear()
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertEquals(
            listOf("createAccount(g1, Visa, CreditCard, -25050)", "getAccounts(g1)", "getAccountsTotal(g1)"),
            repository.calls,
        )
        assertEquals(AccountsSheet.None, viewModel.state.value.sheet)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun emptyOpeningBalanceMeansZero() {
        loaded()
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.NameChanged("Savings"))
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertTrue("createAccount(g1, Savings, Bank, 0)" in repository.calls)
    }

    @Test
    fun invalidInputIsRejectedWithoutCallingTheRepository() {
        loaded()
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.NameChanged("  "))
        viewModel.onEvent(AccountsEvent.OpeningBalanceChanged("1.298,15"))
        repository.calls.clear()
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertEquals(Res.string.validation_required, viewModel.state.value.nameError)
        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.openingBalanceError)
        assertTrue(repository.calls.isEmpty())
        assertEquals(AccountsSheet.Create, viewModel.state.value.sheet)
    }

    @Test
    fun failedCreateKeepsTheSheetOpenWithTheError() {
        repository.createResult = Result.failure(AppError.DuplicateName)
        loaded()
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.NameChanged("Bank"))
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertEquals(AccountsSheet.Create, viewModel.state.value.sheet)
        assertEquals(AppError.DuplicateName, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editFillsTheSheetAndSavesNameAndKind() {
        loaded()
        viewModel.onEvent(AccountsEvent.EditClicked("2"))

        assertEquals(AccountsSheet.Edit, viewModel.state.value.sheet)
        assertEquals("Cash", viewModel.state.value.name)
        assertEquals(AccountKind.Cash, viewModel.state.value.kind)

        viewModel.onEvent(AccountsEvent.NameChanged("Wallet"))
        viewModel.onEvent(AccountsEvent.KindSelected(AccountKind.Savings))
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertTrue("updateAccount(2, Wallet, Savings)" in repository.calls)
        assertEquals(AccountsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun failedEditShowsTheErrorInTheSheet() {
        repository.updateResult = Result.failure(AppError.BusinessRule("FZ016"))
        loaded()
        viewModel.onEvent(AccountsEvent.EditClicked("1"))
        viewModel.onEvent(AccountsEvent.ConfirmClicked)

        assertEquals(AccountsSheet.Edit, viewModel.state.value.sheet)
        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
    }

    @Test
    fun deleteArchivesAtOnceHidesTheRowAndOffersUndo() {
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))

        assertTrue("archiveAccount(1)" in repository.calls)
        assertEquals(listOf(cash), viewModel.state.value.accounts)
        assertEquals(Res.string.account_deleted, viewModel.state.value.undoMessage?.message)
        assertEquals("Bank", viewModel.state.value.undoMessage?.argument)
    }

    @Test
    fun undoUnarchivesAndBringsTheRowBack() {
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))
        viewModel.onEvent(AccountsEvent.UndoClicked)

        assertTrue("unarchiveAccount(1)" in repository.calls)
        assertEquals(listOf(bank, cash), viewModel.state.value.accounts)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun expiredSnackbarMakesNoFurtherCall() {
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))
        viewModel.onEvent(AccountsEvent.UndoExpired)

        assertFalse("unarchiveAccount(1)" in repository.calls)
        assertEquals(listOf(cash), viewModel.state.value.accounts)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun deleteRefusedByTheDatabaseRestoresTheRowWithTheError() {
        repository.archiveResult = Result.failure(AppError.BusinessRule("FZ008"))
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))

        assertEquals(listOf(bank, cash), viewModel.state.value.accounts)
        assertEquals(AppError.BusinessRule("FZ008"), viewModel.state.value.actionError)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun failedUndoKeepsTheRowHiddenAndShowsTheError() {
        repository.unarchiveResult = Result.failure(AppError.Network)
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))
        viewModel.onEvent(AccountsEvent.UndoClicked)

        assertEquals(listOf(cash), viewModel.state.value.accounts)
        assertEquals(AppError.Network, viewModel.state.value.actionError)
    }

    @Test
    fun membersCannotOpenTheSheetsNorDelete() {
        loaded(canEdit = false)
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.EditClicked("1"))
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))

        assertEquals(AccountsSheet.None, viewModel.state.value.sheet)
        assertEquals(listOf(bank, cash), viewModel.state.value.accounts)
        assertTrue(repository.calls.none { it.startsWith("archive") })
    }

    @Test
    fun dismissingTheSheetClosesIt() {
        loaded()
        viewModel.onEvent(AccountsEvent.NewAccountClicked)
        viewModel.onEvent(AccountsEvent.SheetDismissed)

        assertEquals(AccountsSheet.None, viewModel.state.value.sheet)
    }
}
