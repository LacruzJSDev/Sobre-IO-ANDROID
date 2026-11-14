package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_deleted
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
    fun membersCannotDelete() {
        loaded(canEdit = false)
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))

        assertEquals(listOf(bank, cash), viewModel.state.value.accounts)
        assertTrue(repository.calls.none { it.startsWith("archive") })
    }

}
