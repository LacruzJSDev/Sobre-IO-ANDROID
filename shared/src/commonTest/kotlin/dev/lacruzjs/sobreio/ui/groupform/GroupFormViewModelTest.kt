package dev.lacruzjs.sobreio.ui.groupform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import dev.lacruzjs.sobreio.ui.groups.GroupFormState
import dev.lacruzjs.sobreio.ui.groups.MAX_GROUP_NAME_LENGTH
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupFormViewModelTest : MainDispatcherTest() {

    private val casa = Group(
        "g1", "Casa", "EUR", GroupRole.Owner,
        color = "#3B6E8F", icon = "home", timezone = "Europe/Madrid", periodStartDay = 3,
    )
    private val repository = FakeGroupRepository(listOf(casa))

    private fun creating() = GroupFormViewModel(null, repository)
    private fun editing() = GroupFormViewModel("g1", repository)

    private fun GroupFormViewModel.change(change: GroupFormState.() -> GroupFormState) =
        onEvent(GroupFormEvent.FormChanged(state.value.form.change()))

    @Test
    fun createSendsEveryFieldAndAsksToLeaveWithTheNewId() {
        val viewModel = creating()
        viewModel.change {
            copy(name = "  Casa  ", currency = "USD", color = "#3B6E8F", icon = "home", periodStartDay = 5)
        }
        viewModel.onEvent(GroupFormEvent.TimeZonePicked("America/New_York"))
        viewModel.onEvent(GroupFormEvent.SaveClicked)

        assertEquals(listOf("createGroup(Casa, USD, America/New_York, 5, #3B6E8F, home)"), repository.calls)
        assertEquals("new-id", viewModel.state.value.savedGroupId)

        viewModel.onEvent(GroupFormEvent.NavigationHandled)
        assertNull(viewModel.state.value.savedGroupId)
    }

    @Test
    fun createDefaultsToTheDeviceTimeZoneDayOneAndNoAppearance() {
        val viewModel = creating()
        viewModel.change { copy(name = "Casa") }
        viewModel.onEvent(GroupFormEvent.SaveClicked)

        assertEquals(listOf("createGroup(Casa, EUR, ${TimeZone.currentSystemDefault().id}, 1, null, null)"), repository.calls)
    }

    @Test
    fun blankAndTooLongNamesAreRejectedWithoutCallingTheRepository() {
        val viewModel = creating()
        viewModel.change { copy(name = "   ") }
        viewModel.onEvent(GroupFormEvent.SaveClicked)
        assertEquals(Res.string.validation_required, viewModel.state.value.form.nameError)

        viewModel.change { copy(name = "a".repeat(MAX_GROUP_NAME_LENGTH + 1)) }
        viewModel.onEvent(GroupFormEvent.SaveClicked)
        assertEquals(Res.string.validation_name_too_long, viewModel.state.value.form.nameError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun failedCreateStaysOnTheFormWithTheError() {
        repository.createResult = Result.failure(AppError.Network)
        val viewModel = creating()
        viewModel.change { copy(name = "Casa") }
        viewModel.onEvent(GroupFormEvent.SaveClicked)

        assertEquals(AppError.Network, viewModel.state.value.submitError)
        assertNull(viewModel.state.value.savedGroupId)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun editLoadsEveryFieldAndSavesThemAll() {
        val viewModel = editing()
        val form = viewModel.state.value.form
        assertEquals("Casa", form.name)
        assertEquals("#3B6E8F", form.color)
        assertEquals("home", form.icon)
        assertEquals(3, form.periodStartDay)
        assertEquals("Europe/Madrid", form.timezone)
        assertFalse(viewModel.state.value.isLoading)

        viewModel.change { copy(name = " Hogar ", color = null, icon = "key", periodStartDay = 10) }
        viewModel.onEvent(GroupFormEvent.TimeZonePicked("Europe/London"))
        viewModel.onEvent(GroupFormEvent.SaveClicked)

        assertTrue("updateGroup(g1, Hogar, Europe/London, 10, null, key)" in repository.calls)
        assertEquals("g1", viewModel.state.value.savedGroupId)
    }

    @Test
    fun editOfAMissingGroupShowsNotFoundAndDoesNotSave() {
        val viewModel = GroupFormViewModel("gone", repository)

        assertEquals(AppError.NotFound, viewModel.state.value.loadError)
        viewModel.onEvent(GroupFormEvent.SaveClicked)
        assertTrue(repository.calls.none { it.startsWith("updateGroup") })
    }

    @Test
    fun failedEditStaysOnTheFormWithTheError() {
        repository.updateResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = editing()
        viewModel.onEvent(GroupFormEvent.SaveClicked)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertNull(viewModel.state.value.savedGroupId)
    }
}
