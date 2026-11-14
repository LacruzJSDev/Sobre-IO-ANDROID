package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileViewModelTest : MainDispatcherTest() {

    private val user = User("1", "ana@example.com", "Ana")
    private val auth = FakeAuthRepository(AuthState.SignedIn(user))
    // Lazy: init launches a coroutine, which needs Main replaced first
    private val viewModel by lazy { ProfileViewModel(auth) }

    @Test
    fun showsTheSignedInUserAndFillsTheNameField() {
        assertEquals(user, viewModel.state.value.user)
        assertEquals("Ana", viewModel.state.value.name)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun savingSendsTheTrimmedNameAndShowsTheUpdatedUser() {
        viewModel.onEvent(ProfileEvent.NameChanged("  Ana María "))
        assertTrue(viewModel.state.value.isDirty)
        viewModel.onEvent(ProfileEvent.SaveClicked)

        assertEquals(listOf("updateDisplayName(Ana María)"), auth.calls)
        assertEquals("Ana María", viewModel.state.value.user?.displayName)
        assertEquals("Ana María", viewModel.state.value.name)
        assertTrue(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSaving)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun editingAgainClearsTheSavedMessage() {
        viewModel.onEvent(ProfileEvent.NameChanged("Marta"))
        viewModel.onEvent(ProfileEvent.SaveClicked)
        viewModel.onEvent(ProfileEvent.NameChanged("Marta B"))

        assertFalse(viewModel.state.value.saved)
    }

    @Test
    fun aBlankNameIsRejectedWithoutCallingTheRepository() {
        viewModel.onEvent(ProfileEvent.NameChanged("   "))
        viewModel.onEvent(ProfileEvent.SaveClicked)

        assertEquals(Res.string.validation_required, viewModel.state.value.nameError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun aTooLongNameIsRejected() {
        viewModel.onEvent(ProfileEvent.NameChanged("a".repeat(MAX_DISPLAY_NAME_LENGTH + 1)))
        viewModel.onEvent(ProfileEvent.SaveClicked)

        assertEquals(Res.string.validation_name_too_long, viewModel.state.value.nameError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun aFailedSaveShowsTheErrorAndKeepsTheOldName() {
        auth.result = Result.failure(AppError.Network)
        viewModel.onEvent(ProfileEvent.NameChanged("Marta"))
        viewModel.onEvent(ProfileEvent.SaveClicked)

        assertEquals(AppError.Network, viewModel.state.value.saveError)
        assertEquals("Ana", viewModel.state.value.user?.displayName)
        assertFalse(viewModel.state.value.saved)
        assertFalse(viewModel.state.value.isSaving)
    }
}
