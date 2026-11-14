package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountsSummary
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_deleted
import dev.lacruzjs.sobreio.generated.resources.account_reopened
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountsViewModelTest : MainDispatcherTest() {

    private val bank = Account("1", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val savings = Account("3", "g1", "Savings", AccountKind.Savings, false, 50_000, color = "#4F7A5A", icon = "savings")
    private val old = Account("4", "g1", "Old", AccountKind.Bank, true, 0, archivedAt = "2026-11-01T10:00:00Z")
    private val repository = FakeAccountRepository(listOf(bank, cash))
        .also { it.summary = AccountsSummary(totalCents = 105_000, inEnvelopesCents = 105_000, outsideCents = 0) }
    private val memberRepository = FakeMemberRepository()
    private val viewModel by lazy { AccountsViewModel("g1", repository, memberRepository) }

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

    @Test
    fun accountsAreSplitIntoInsideAndOutsideTheEnvelopesWithTheDatabaseFigures() {
        repository.accounts = listOf(bank, cash, savings)
        repository.summary = AccountsSummary(totalCents = 155_000, inEnvelopesCents = 105_000, outsideCents = 50_000)
        loaded()

        val state = viewModel.state.value
        assertEquals(listOf(bank, cash), state.inEnvelopes)
        assertEquals(listOf(savings), state.outsideEnvelopes)
        assertEquals(155_000, state.totalCents)
        assertEquals(105_000, state.inEnvelopesCents)
        assertEquals(50_000, state.outsideCents)
        assertEquals("#4F7A5A", state.outsideEnvelopes.single().color)
    }

    @Test
    fun archivedAccountsGoToTheArchivedSectionNotTheList() {
        repository.accounts = listOf(bank, old, cash)
        loaded()

        assertEquals(listOf(bank, cash), viewModel.state.value.accounts)
        assertEquals(listOf(old), viewModel.state.value.archivedAccounts)
    }

    @Test
    fun reopenUnarchivesMovesTheRowToTheListAndOffersUndo() {
        repository.accounts = listOf(bank, old, cash)
        loaded()
        viewModel.onEvent(AccountsEvent.ReopenClicked("4"))

        assertTrue("unarchiveAccount(4)" in repository.calls)
        assertEquals(listOf("Bank", "Cash", "Old"), viewModel.state.value.accounts.map { it.name })
        assertTrue(viewModel.state.value.archivedAccounts.isEmpty())
        assertEquals(Res.string.account_reopened, viewModel.state.value.undoMessage?.message)
        assertEquals("Old", viewModel.state.value.undoMessage?.argument)
    }

    @Test
    fun undoOfReopenArchivesTheAccountAgain() {
        repository.accounts = listOf(bank, old)
        loaded()
        viewModel.onEvent(AccountsEvent.ReopenClicked("4"))
        repository.accounts = listOf(bank, old)
        viewModel.onEvent(AccountsEvent.UndoClicked)

        assertTrue("archiveAccount(4)" in repository.calls)
        assertEquals(listOf(old), viewModel.state.value.archivedAccounts)
    }

    @Test
    fun reopenRefusedByTheDatabaseKeepsTheRowArchivedWithTheError() {
        repository.accounts = listOf(bank, old)
        repository.unarchiveResult = Result.failure(AppError.DuplicateName)
        loaded()
        viewModel.onEvent(AccountsEvent.ReopenClicked("4"))

        assertEquals(listOf(old), viewModel.state.value.archivedAccounts)
        assertEquals(AppError.DuplicateName, viewModel.state.value.actionError)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun membersCannotReopen() {
        repository.accounts = listOf(bank, old)
        loaded(canEdit = false)
        viewModel.onEvent(AccountsEvent.ReopenClicked("4"))

        assertTrue(repository.calls.none { it.startsWith("unarchive") })
    }

    @Test
    fun deletedAccountAppearsInTheArchivedSectionAndComesBackOnUndo() {
        loaded()
        viewModel.onEvent(AccountsEvent.DeleteClicked("1"))
        assertEquals(listOf("Bank"), viewModel.state.value.archivedAccounts.map { it.name })

        viewModel.onEvent(AccountsEvent.UndoClicked)
        assertTrue(viewModel.state.value.archivedAccounts.isEmpty())
    }

    @Test
    fun holderNamesAreOnlyShownWhenTheGroupHasSeveralMembers() {
        val shared = bank.copy(ownerUserId = "u2")
        repository.accounts = listOf(shared, cash)
        memberRepository.members = listOf(Member("u1", "Juan", GroupRole.Owner))
        loaded()
        assertFalse(viewModel.state.value.hasSeveralMembers)
        assertNull(viewModel.state.value.holderName(shared))

        memberRepository.members = listOf(Member("u1", "Juan", GroupRole.Owner), Member("u2", "Marta", GroupRole.Member))
        viewModel.onEvent(AccountsEvent.Refresh)
        assertTrue(viewModel.state.value.hasSeveralMembers)
        assertEquals("Marta", viewModel.state.value.holderName(shared))
        assertNull(viewModel.state.value.holderName(cash))
    }
}
