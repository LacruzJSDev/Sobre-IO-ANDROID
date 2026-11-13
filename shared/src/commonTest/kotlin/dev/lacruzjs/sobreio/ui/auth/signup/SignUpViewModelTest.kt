package dev.lacruzjs.sobreio.ui.auth.signup

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_email_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_password_short
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignUpViewModelTest : MainDispatcherTest() {

    private val auth = FakeAuthRepository()
    private val viewModel = SignUpViewModel(auth)

    private fun fill(name: String = " Ana ", email: String = "ana@example.com", password: String = "secret1") {
        viewModel.onEvent(SignUpEvent.NameChanged(name))
        viewModel.onEvent(SignUpEvent.EmailChanged(email))
        viewModel.onEvent(SignUpEvent.PasswordChanged(password))
    }

    @Test
    fun invalidFieldsShowTheirErrorsAndDoNotCallTheRepository() {
        fill(name = " ", email = "nope", password = "123")
        viewModel.onEvent(SignUpEvent.SignUpClicked)

        val state = viewModel.state.value
        assertEquals(Res.string.validation_required, state.nameError)
        assertEquals(Res.string.validation_email_invalid, state.emailError)
        assertEquals(Res.string.validation_password_short, state.passwordError)
        assertEquals(emptyList<String>(), auth.calls)
    }

    @Test
    fun successfulSignUpAsksToShowTheCodeScreenForThatEmail() {
        fill()
        viewModel.onEvent(SignUpEvent.SignUpClicked)

        assertEquals(listOf("signUp(Ana, ana@example.com, secret1)"), auth.calls)
        assertEquals("ana@example.com", viewModel.state.value.navigateToConfirm)
    }

    @Test
    fun failedSignUpShowsTheErrorAndDoesNotNavigate() {
        auth.result = Result.failure(AppError.EmailAlreadyRegistered)
        fill()
        viewModel.onEvent(SignUpEvent.SignUpClicked)

        assertEquals(AppError.EmailAlreadyRegistered, viewModel.state.value.error)
        assertNull(viewModel.state.value.navigateToConfirm)
    }

    @Test
    fun navigationHandledClearsThePendingNavigation() {
        fill()
        viewModel.onEvent(SignUpEvent.SignUpClicked)
        viewModel.onEvent(SignUpEvent.NavigationHandled)

        assertNull(viewModel.state.value.navigateToConfirm)
    }
}
