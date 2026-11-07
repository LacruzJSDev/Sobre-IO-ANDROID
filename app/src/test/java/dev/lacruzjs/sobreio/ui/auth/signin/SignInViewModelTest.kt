package dev.lacruzjs.sobreio.ui.auth.signin

import dev.lacruzjs.sobreio.R
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = SignInViewModel(auth, isGoogleAvailable = true)

    private fun fill(email: String = " ana@example.com ", password: String = "secret1") {
        viewModel.onEvent(SignInEvent.EmailChanged(email))
        viewModel.onEvent(SignInEvent.PasswordChanged(password))
    }

    @Test
    fun emptyFieldsShowRequiredErrorsAndDoNotCallTheRepository() {
        viewModel.onEvent(SignInEvent.SignInClicked)

        val state = viewModel.state.value
        assertEquals(R.string.validation_required, state.emailError)
        assertEquals(R.string.validation_required, state.passwordError)
        assertEquals(emptyList<String>(), auth.calls)
    }

    @Test
    fun malformedEmailIsRejectedBeforeCallingTheRepository() {
        fill(email = "ana@")
        viewModel.onEvent(SignInEvent.SignInClicked)

        assertEquals(R.string.validation_email_invalid, viewModel.state.value.emailError)
        assertEquals(emptyList<String>(), auth.calls)
    }

    @Test
    fun signInSendsTrimmedEmailAndStopsLoading() {
        fill()
        viewModel.onEvent(SignInEvent.SignInClicked)

        assertEquals(listOf("signIn(ana@example.com, secret1)"), auth.calls)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun failedSignInShowsTheError() {
        auth.result = Result.failure(AppError.InvalidCredentials)
        fill()
        viewModel.onEvent(SignInEvent.SignInClicked)

        assertEquals(AppError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun editingAFieldClearsItsErrorAndTheGeneralError() {
        auth.result = Result.failure(AppError.InvalidCredentials)
        fill()
        viewModel.onEvent(SignInEvent.SignInClicked)
        viewModel.onEvent(SignInEvent.PasswordChanged("other"))

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun unconfirmedEmailResendsTheCodeAndAsksToShowTheCodeScreen() {
        auth.result = Result.failure(AppError.EmailNotConfirmed)
        fill()
        viewModel.onEvent(SignInEvent.SignInClicked)
        auth.result = Result.success(Unit)
        viewModel.onEvent(SignInEvent.ConfirmEmailClicked)

        assertEquals("ana@example.com", viewModel.state.value.navigateToConfirm)
        assertEquals("resendSignUpCode(ana@example.com)", auth.calls.last())
    }

    @Test
    fun rateLimitedResendStillGoesToTheCodeScreen() {
        fill()
        auth.result = Result.failure(AppError.TooManyRequests)
        viewModel.onEvent(SignInEvent.ConfirmEmailClicked)

        assertEquals("ana@example.com", viewModel.state.value.navigateToConfirm)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun otherResendFailureShowsTheErrorInstead() {
        fill()
        auth.result = Result.failure(AppError.Network)
        viewModel.onEvent(SignInEvent.ConfirmEmailClicked)

        assertNull(viewModel.state.value.navigateToConfirm)
        assertEquals(AppError.Network, viewModel.state.value.error)
    }

    @Test
    fun navigationHandledClearsThePendingNavigation() {
        fill()
        viewModel.onEvent(SignInEvent.ConfirmEmailClicked)
        viewModel.onEvent(SignInEvent.NavigationHandled)

        assertNull(viewModel.state.value.navigateToConfirm)
    }

    @Test
    fun googleTokenIsPassedToTheRepository() {
        viewModel.onEvent(SignInEvent.GoogleTokenReceived("token", "nonce"))

        assertEquals(listOf("signInWithGoogle(token, nonce)"), auth.calls)
    }

    @Test
    fun googleFailureShowsAGenericError() {
        viewModel.onEvent(SignInEvent.GoogleFailed)

        assertEquals(AppError.Unknown(), viewModel.state.value.error)
    }
}
