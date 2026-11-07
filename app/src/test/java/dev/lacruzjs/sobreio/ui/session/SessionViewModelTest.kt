package dev.lacruzjs.sobreio.ui.session

import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun followsTheRepositoryAuthState() {
        val auth = FakeAuthRepository(AuthState.Loading)
        val viewModel = SessionViewModel(auth)
        assertEquals(AuthState.Loading, viewModel.state.value)

        auth.state.value = AuthState.SignedOut
        assertEquals(AuthState.SignedOut, viewModel.state.value)

        val signedIn = AuthState.SignedIn(User("1", "ana@example.com", "Ana"))
        auth.state.value = signedIn
        assertEquals(signedIn, viewModel.state.value)
    }
}
