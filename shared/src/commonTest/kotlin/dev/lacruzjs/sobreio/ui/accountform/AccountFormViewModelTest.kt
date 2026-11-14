package dev.lacruzjs.sobreio.ui.accountform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountDraft
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_account_scope_movements
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountFormViewModelTest : MainDispatcherTest() {

    private val cash = Account(
        "2", "g1", "Cash", AccountKind.Cash, true, 5_000,
        color = "#3B6E8F", icon = "wallet", ownerUserId = "u2",
    )
    private val repository = FakeAccountRepository(listOf(cash))
    private val juan = Member("u1", "Juan", GroupRole.Owner)
    private val marta = Member("u2", "Marta", GroupRole.Member)
    private val memberRepository = FakeMemberRepository(listOf(juan, marta))

    private fun form(accountId: String? = null) = AccountFormViewModel("g1", accountId, repository, memberRepository)

    @Test
    fun createSendsEveryFieldInOneCallAndAsksToLeave() {
        val viewModel = form()
        viewModel.onEvent(AccountFormEvent.NameChanged("  Visa  "))
        viewModel.onEvent(AccountFormEvent.KindSelected(AccountKind.CreditCard))
        viewModel.onEvent(AccountFormEvent.ColorSelected("#8E3B4A"))
        viewModel.onEvent(AccountFormEvent.IconSelected("credit_card"))
        viewModel.onEvent(AccountFormEvent.HolderSelected("u1"))
        viewModel.onEvent(AccountFormEvent.InEnvelopesChanged(false))
        viewModel.onEvent(AccountFormEvent.OpeningBalanceChanged("-250,5"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(listOf("createAccount(g1, Visa, CreditCard, -25050)"), repository.calls)
        assertEquals(AccountDraft("Visa", AccountKind.CreditCard, "u1", "#8E3B4A", "credit_card"), repository.createdDraft)
        assertEquals(false, repository.createdInEnvelopes)
        assertEquals(-25050L, repository.createdOpeningBalance)
        assertTrue(viewModel.state.value.saved)
        viewModel.onEvent(AccountFormEvent.NavigationHandled)
        assertFalse(viewModel.state.value.saved)
    }

    @Test
    fun newAccountsAreInTheEnvelopesJointAndHaveNoColourOrIconByDefault() {
        val viewModel = form()
        viewModel.onEvent(AccountFormEvent.NameChanged("Savings"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertTrue("createAccount(g1, Savings, Bank, 0)" in repository.calls)
        assertEquals(AccountDraft("Savings", AccountKind.Bank, null, null, null), repository.createdDraft)
        assertEquals(true, repository.createdInEnvelopes)
    }

    @Test
    fun invalidInputIsRejectedWithoutCallingTheRepository() {
        val viewModel = form()
        viewModel.onEvent(AccountFormEvent.NameChanged("  "))
        viewModel.onEvent(AccountFormEvent.OpeningBalanceChanged("1.298,15"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(Res.string.validation_required, viewModel.state.value.nameError)
        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.openingBalanceError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun failedCreateStaysOnTheFormWithTheError() {
        repository.createResult = Result.failure(AppError.DuplicateName)
        val viewModel = form()
        viewModel.onEvent(AccountFormEvent.NameChanged("Bank"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(AppError.DuplicateName, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editLoadsEveryFieldAndSavesThemWithoutTouchingTheScopeWhenItDidNotChange() {
        val viewModel = form("2")
        val loaded = viewModel.state.value
        assertEquals("Cash", loaded.name)
        assertEquals(AccountKind.Cash, loaded.kind)
        assertEquals("#3B6E8F", loaded.color)
        assertEquals("wallet", loaded.icon)
        assertEquals("u2", loaded.ownerUserId)
        assertTrue(loaded.inEnvelopes)

        viewModel.onEvent(AccountFormEvent.NameChanged("Wallet"))
        viewModel.onEvent(AccountFormEvent.KindSelected(AccountKind.Savings))
        viewModel.onEvent(AccountFormEvent.ColorSelected(null))
        viewModel.onEvent(AccountFormEvent.HolderSelected(null))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(listOf("getAccounts(g1)", "updateAccount(2, Wallet, Savings)"), repository.calls)
        assertEquals(AccountDraft("Wallet", AccountKind.Savings, null, null, "wallet"), repository.updatedDraft)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun editChangesTheScopeThroughItsOwnCallBeforeTheUpdate() {
        val viewModel = form("2")
        viewModel.onEvent(AccountFormEvent.InEnvelopesChanged(false))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(
            listOf("getAccounts(g1)", "changeAccountScope(2, false)", "updateAccount(2, Cash, Cash)"),
            repository.calls,
        )
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun scopeRefusedBecauseOfMovementsShowsTheMessageAndSavesNothing() {
        repository.scopeResult = Result.failure(AppError.BusinessRule("FZ017"))
        val viewModel = form("2")
        viewModel.onEvent(AccountFormEvent.InEnvelopesChanged(false))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(Res.string.error_account_scope_movements, viewModel.state.value.scopeError)
        assertNull(viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)
        assertTrue(repository.calls.none { it.startsWith("updateAccount") })
    }

    @Test
    fun otherScopeFailuresAreReportedAsTheFormError() {
        repository.scopeResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = form("2")
        viewModel.onEvent(AccountFormEvent.InEnvelopesChanged(false))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertNull(viewModel.state.value.scopeError)
    }

    @Test
    fun retryAfterAFailedUpdateDoesNotRepeatTheScopeChange() {
        repository.updateResult = Result.failure(AppError.Network)
        val viewModel = form("2")
        viewModel.onEvent(AccountFormEvent.InEnvelopesChanged(false))
        viewModel.onEvent(AccountFormEvent.SaveClicked)
        assertEquals(AppError.Network, viewModel.state.value.submitError)

        repository.updateResult = Result.success(Unit)
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(1, repository.calls.count { it.startsWith("changeAccountScope") })
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun theHolderIsOnlyAskedForWhenTheGroupHasSeveralMembers() {
        assertTrue(form().state.value.hasSeveralMembers)

        memberRepository.members = listOf(juan)
        assertFalse(form().state.value.hasSeveralMembers)
    }

    @Test
    fun editOfAMissingAccountShowsNotFound() {
        assertEquals(AppError.NotFound, form("nope").state.value.loadError)
    }

    @Test
    fun failedEditShowsTheError() {
        repository.updateResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = form("2")
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
    }
}
