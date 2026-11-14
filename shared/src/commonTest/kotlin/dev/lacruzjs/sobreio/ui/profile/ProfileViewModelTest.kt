package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileViewModelTest : MainDispatcherTest() {

    private val user = User("1", "ana@example.com", "Ana")
    private val auth = FakeAuthRepository(AuthState.SignedIn(user))
    // Lazy: init launches a coroutine, which needs Main replaced first
    private val viewModel by lazy { ProfileViewModel(auth) }

    @Test
    fun showsTheSignedInUser() {
        assertEquals(user, viewModel.state.value.user)
    }
}
