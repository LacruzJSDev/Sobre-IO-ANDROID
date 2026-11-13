package dev.lacruzjs.sobreio.ui.home

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HomeViewModelTest : MainDispatcherTest() {

    private val user = User("1", "ana@example.com", "Ana")
    private val auth = FakeAuthRepository(AuthState.SignedIn(user))
    // Lazy: init launches a coroutine, which needs Main replaced first
    private val viewModel by lazy { HomeViewModel(auth) }

    @Test
    fun showsTheSignedInUser() {
        assertEquals(user, viewModel.state.value.user)
    }

    @Test
    fun signOutCallsTheRepository() {
        viewModel.onEvent(HomeEvent.SignOutClicked)

        assertEquals(listOf("signOut()"), auth.calls)
        assertFalse(viewModel.state.value.isSigningOut)
    }

    @Test
    fun failedSignOutShowsTheError() {
        auth.result = Result.failure(AppError.Network)
        viewModel.onEvent(HomeEvent.SignOutClicked)

        assertEquals(AppError.Network, viewModel.state.value.error)
    }
}
