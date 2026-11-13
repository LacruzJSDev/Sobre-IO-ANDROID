package dev.lacruzjs.sobreio.ui.auth.confirm

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_code_invalid
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmEmailViewModelTest : MainDispatcherTest() {

    private val auth = FakeAuthRepository()
    private lateinit var viewModel: ConfirmEmailViewModel
    private val scheduler get() = testDispatcher.scheduler

    // Created after Main has been replaced, because init starts the cooldown coroutine
    @BeforeTest
    fun createViewModel() {
        viewModel = ConfirmEmailViewModel("ana@example.com", auth)
    }

    private fun advanceSeconds(seconds: Int) {
        scheduler.advanceTimeBy(seconds * 1_000L)
        scheduler.runCurrent()
    }

    @Test
    fun codeKeepsOnlySixDigits() {
        viewModel.onEvent(ConfirmEmailEvent.CodeChanged("12a3-4567890"))

        assertEquals("123456", viewModel.state.value.code)
    }

    @Test
    fun shortCodeShowsValidationErrorWithoutCallingTheRepository() {
        viewModel.onEvent(ConfirmEmailEvent.CodeChanged("123"))
        viewModel.onEvent(ConfirmEmailEvent.ConfirmClicked)

        assertEquals(Res.string.validation_code_invalid, viewModel.state.value.codeError)
        assertEquals(emptyList<String>(), auth.calls)
    }

    @Test
    fun validCodeIsSentWithTheEmail() {
        viewModel.onEvent(ConfirmEmailEvent.CodeChanged("123456"))
        viewModel.onEvent(ConfirmEmailEvent.ConfirmClicked)

        assertEquals(listOf("confirmSignUp(ana@example.com, 123456)"), auth.calls)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun wrongCodeShowsTheError() {
        auth.result = Result.failure(AppError.InvalidCode)
        viewModel.onEvent(ConfirmEmailEvent.CodeChanged("123456"))
        viewModel.onEvent(ConfirmEmailEvent.ConfirmClicked)

        assertEquals(AppError.InvalidCode, viewModel.state.value.error)
    }

    @Test
    fun resendIsBlockedUntilTheCooldownEnds() {
        assertEquals(RESEND_COOLDOWN_SECONDS, viewModel.state.value.resendSecondsLeft)
        viewModel.onEvent(ConfirmEmailEvent.ResendClicked)
        assertEquals(emptyList<String>(), auth.calls)

        advanceSeconds(30)
        assertEquals(RESEND_COOLDOWN_SECONDS - 30, viewModel.state.value.resendSecondsLeft)

        advanceSeconds(30)
        assertEquals(0, viewModel.state.value.resendSecondsLeft)
    }

    @Test
    fun resendAfterTheCooldownSendsANewCodeAndRestartsTheCooldown() {
        advanceSeconds(RESEND_COOLDOWN_SECONDS)
        viewModel.onEvent(ConfirmEmailEvent.ResendClicked)

        assertEquals(listOf("resendSignUpCode(ana@example.com)"), auth.calls)
        assertTrue(viewModel.state.value.codeResent)
        assertEquals(RESEND_COOLDOWN_SECONDS, viewModel.state.value.resendSecondsLeft)
    }

    @Test
    fun failedResendShowsTheErrorAndKeepsTheButtonEnabled() {
        advanceSeconds(RESEND_COOLDOWN_SECONDS)
        auth.result = Result.failure(AppError.TooManyRequests)
        viewModel.onEvent(ConfirmEmailEvent.ResendClicked)

        assertEquals(AppError.TooManyRequests, viewModel.state.value.error)
        assertEquals(0, viewModel.state.value.resendSecondsLeft)
    }
}
