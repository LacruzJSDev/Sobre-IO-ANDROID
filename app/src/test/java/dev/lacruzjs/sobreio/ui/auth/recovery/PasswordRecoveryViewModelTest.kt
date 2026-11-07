package dev.lacruzjs.sobreio.ui.auth.recovery

import dev.lacruzjs.sobreio.R
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PasswordRecoveryViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = PasswordRecoveryViewModel(auth)

    private fun goToResetStep() {
        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("ana@example.com"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)
    }

    @Test
    fun invalidEmailStaysOnTheEmailStep() {
        viewModel.onEvent(PasswordRecoveryEvent.EmailChanged("nope"))
        viewModel.onEvent(PasswordRecoveryEvent.SendCodeClicked)

        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
        assertEquals(R.string.validation_email_invalid, viewModel.state.value.emailError)
        assertEquals(emptyList<String>(), auth.calls)
    }

    @Test
    fun sendingTheCodeMovesToTheResetStep() {
        goToResetStep()

        assertEquals(listOf("requestPasswordReset(ana@example.com)"), auth.calls)
        assertEquals(RecoveryStep.Reset, viewModel.state.value.step)
    }

    @Test
    fun failureSendingTheCodeStaysOnTheEmailStepWithTheError() {
        auth.result = Result.failure(AppError.Network)
        goToResetStep()

        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
        assertEquals(AppError.Network, viewModel.state.value.error)
    }

    @Test
    fun resendingTheCodeCallsTheRepositoryAgainAndSaysSo() {
        goToResetStep()
        viewModel.onEvent(PasswordRecoveryEvent.ResendCodeClicked)

        assertEquals(2, auth.calls.size)
        assertTrue(viewModel.state.value.codeResent)
    }

    @Test
    fun changingTheEmailGoesBackToTheFirstStep() {
        goToResetStep()
        viewModel.onEvent(PasswordRecoveryEvent.ChangeEmailClicked)

        assertEquals(RecoveryStep.Email, viewModel.state.value.step)
    }

    @Test
    fun resetStepValidatesCodePasswordLengthAndMatch() {
        goToResetStep()
        viewModel.onEvent(PasswordRecoveryEvent.CodeChanged("12"))
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("123"))
        viewModel.onEvent(PasswordRecoveryEvent.RepeatedPasswordChanged("124"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetClicked)

        val state = viewModel.state.value
        assertEquals(R.string.validation_code_invalid, state.codeError)
        assertEquals(R.string.validation_password_short, state.newPasswordError)
        assertEquals(R.string.validation_passwords_mismatch, state.repeatedPasswordError)
        assertEquals(1, auth.calls.size)
    }

    @Test
    fun validResetSendsCodeAndNewPassword() {
        goToResetStep()
        viewModel.onEvent(PasswordRecoveryEvent.CodeChanged("123456"))
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("newsecret"))
        viewModel.onEvent(PasswordRecoveryEvent.RepeatedPasswordChanged("newsecret"))
        viewModel.onEvent(PasswordRecoveryEvent.ResetClicked)

        assertEquals("resetPassword(ana@example.com, 123456, newsecret)", auth.calls.last())
    }

    @Test
    fun wrongCodeOnResetShowsTheError() {
        goToResetStep()
        viewModel.onEvent(PasswordRecoveryEvent.CodeChanged("123456"))
        viewModel.onEvent(PasswordRecoveryEvent.NewPasswordChanged("newsecret"))
        viewModel.onEvent(PasswordRecoveryEvent.RepeatedPasswordChanged("newsecret"))
        auth.result = Result.failure(AppError.InvalidCode)
        viewModel.onEvent(PasswordRecoveryEvent.ResetClicked)

        assertEquals(AppError.InvalidCode, viewModel.state.value.error)
        assertEquals(RecoveryStep.Reset, viewModel.state.value.step)
    }
}
