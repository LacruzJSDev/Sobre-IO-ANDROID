package dev.lacruzjs.sobreio.ui.accountform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountFormViewModelTest : MainDispatcherTest() {

    private val cash = Account("2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val repository = FakeAccountRepository(listOf(cash))

    @Test
    fun createSendsTrimmedNameKindAndOpeningBalanceAndAsksToLeave() {
        val viewModel = AccountFormViewModel("g1", null, repository)
        viewModel.onEvent(AccountFormEvent.NameChanged("  Visa  "))
        viewModel.onEvent(AccountFormEvent.KindSelected(AccountKind.CreditCard))
        viewModel.onEvent(AccountFormEvent.OpeningBalanceChanged("-250,5"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(listOf("createAccount(g1, Visa, CreditCard, -25050)"), repository.calls)
        assertTrue(viewModel.state.value.saved)
        viewModel.onEvent(AccountFormEvent.NavigationHandled)
        assertFalse(viewModel.state.value.saved)
    }

    @Test
    fun emptyOpeningBalanceMeansZero() {
        val viewModel = AccountFormViewModel("g1", null, repository)
        viewModel.onEvent(AccountFormEvent.NameChanged("Savings"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertTrue("createAccount(g1, Savings, Bank, 0)" in repository.calls)
    }

    @Test
    fun invalidInputIsRejectedWithoutCallingTheRepository() {
        val viewModel = AccountFormViewModel("g1", null, repository)
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
        val viewModel = AccountFormViewModel("g1", null, repository)
        viewModel.onEvent(AccountFormEvent.NameChanged("Bank"))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(AppError.DuplicateName, viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editLoadsTheAccountAndSavesNameAndKind() {
        val viewModel = AccountFormViewModel("g1", "2", repository)
        assertEquals("Cash", viewModel.state.value.name)
        assertEquals(AccountKind.Cash, viewModel.state.value.kind)

        viewModel.onEvent(AccountFormEvent.NameChanged("Wallet"))
        viewModel.onEvent(AccountFormEvent.KindSelected(AccountKind.Savings))
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertTrue("updateAccount(2, Wallet, Savings)" in repository.calls)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun editOfAMissingAccountShowsNotFound() {
        val viewModel = AccountFormViewModel("g1", "nope", repository)

        assertEquals(AppError.NotFound, viewModel.state.value.loadError)
    }

    @Test
    fun failedEditShowsTheError() {
        repository.updateResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = AccountFormViewModel("g1", "2", repository)
        viewModel.onEvent(AccountFormEvent.SaveClicked)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.saved)
    }
}
