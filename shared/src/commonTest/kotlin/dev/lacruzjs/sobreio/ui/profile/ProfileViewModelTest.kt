package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAppPreferencesStore
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeExportRepository
import dev.lacruzjs.sobreio.fake.FakeLegalRepository
import dev.lacruzjs.sobreio.ui.export.SaveResult
import dev.lacruzjs.sobreio.generated.resources.export_saved
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
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
    private val preferences = FakeAppPreferencesStore()
    private val legal = FakeLegalRepository()
    private val exports = FakeExportRepository()
    private val clock = object : Clock { override fun now() = Instant.parse("2026-12-03T10:00:00Z") }
    private val viewModel by lazy { ProfileViewModel(auth, preferences, legal, exports, clock) }

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

    @Test
    fun colourBlindToggleIsReadFromAndWrittenToTheStore() {
        assertFalse(viewModel.state.value.colorBlindSafe)
        viewModel.onEvent(ProfileEvent.ColorBlindSafeChanged(true))
        assertTrue(preferences.colorBlindSafe.value)
        assertTrue(viewModel.state.value.colorBlindSafe)
        viewModel.onEvent(ProfileEvent.ColorBlindSafeChanged(false))
        assertFalse(preferences.colorBlindSafe.value)
        assertFalse(viewModel.state.value.colorBlindSafe)
    }

    @Test
    fun showsTheDocumentsInForceAndWhatWasAccepted() {
        legal.status = Result.success(
            listOf(
                FakeLegalRepository.status(FakeLegalRepository.TERMS, FakeLegalRepository.VERSION, needsAcceptance = false),
                FakeLegalRepository.status(FakeLegalRepository.PRIVACY, FakeLegalRepository.VERSION, needsAcceptance = false),
            ),
        )
        assertEquals(FakeLegalRepository.TERMS, viewModel.state.value.terms?.document)
        assertEquals(FakeLegalRepository.PRIVACY, viewModel.state.value.privacy?.document)
    }

    @Test
    fun aFailedLegalReadLeavesOnlyTheDownload() {
        legal.status = Result.failure(AppError.Network)
        assertNull(viewModel.state.value.terms)
        assertNull(viewModel.state.value.privacy)
    }

    @Test
    fun downloadingReadsTheDataAndHandsItToTheRouteNamedByToday() {
        viewModel.onEvent(ProfileEvent.DownloadDataClicked)
        val data = viewModel.state.value.pendingData
        assertEquals(LocalDate(2026, 12, 3), data?.day)
        assertEquals("""{"format":"sobre-io-export"}""", data?.content)
        assertFalse(viewModel.state.value.isDownloading)
        // A second tap while the dialog is open reads nothing again
        viewModel.onEvent(ProfileEvent.DownloadDataClicked)
        assertEquals(1, exports.myDataCalls)
    }

    @Test
    fun theSaveDialogClosingClearsTheDataAndSaysWhatHappened() {
        viewModel.onEvent(ProfileEvent.DownloadDataClicked)
        viewModel.onEvent(ProfileEvent.DataFileHandled(SaveResult.Saved))
        assertNull(viewModel.state.value.pendingData)
        assertEquals(Res.string.export_saved, viewModel.state.value.downloadMessage)

        viewModel.onEvent(ProfileEvent.DownloadDataClicked)
        viewModel.onEvent(ProfileEvent.DataFileHandled(SaveResult.Cancelled))
        assertNull(viewModel.state.value.downloadMessage)
    }

    @Test
    fun aFailedDownloadShowsTheError() {
        exports.myData = Result.failure(AppError.Network)
        viewModel.onEvent(ProfileEvent.DownloadDataClicked)
        assertEquals(AppError.Network, viewModel.state.value.downloadError)
        assertNull(viewModel.state.value.pendingData)
    }
}
