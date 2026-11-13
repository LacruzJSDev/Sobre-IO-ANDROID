package dev.lacruzjs.sobreio.ui.groups

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupsViewModelTest : MainDispatcherTest() {

    private val repository = FakeGroupRepository(listOf(Group("1", "Casa", "EUR")))
    private val viewModel by lazy { GroupsViewModel(repository) }

    @Test
    fun refreshLoadsTheGroups() {
        viewModel.onEvent(GroupsEvent.Refresh)

        assertEquals(listOf(Group("1", "Casa", "EUR")), viewModel.state.value.groups)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun noGroupsGivesAnEmptyListNotAnError() {
        repository.groups = emptyList()
        viewModel.onEvent(GroupsEvent.Refresh)

        assertTrue(viewModel.state.value.groups.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun failedLoadShowsTheError() {
        repository.groupsResult = Result.failure(AppError.Network)
        viewModel.onEvent(GroupsEvent.Refresh)

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun createSendsTrimmedNameAndCurrencyThenAsksToOpenTheGroup() {
        viewModel.onEvent(GroupsEvent.NewGroupClicked)
        viewModel.onEvent(GroupsEvent.GroupNameChanged("  Casa  "))
        viewModel.onEvent(GroupsEvent.CurrencySelected("USD"))
        viewModel.onEvent(GroupsEvent.CreateConfirmed)

        assertEquals(listOf("createGroup(Casa, USD)"), repository.calls)
        assertEquals("new-id", viewModel.state.value.navigateToGroup)
        assertEquals(GroupsSheet.None, viewModel.state.value.sheet)

        viewModel.onEvent(GroupsEvent.NavigationHandled)
        assertNull(viewModel.state.value.navigateToGroup)
    }

    @Test
    fun blankNameIsRejectedWithoutCallingTheRepository() {
        viewModel.onEvent(GroupsEvent.NewGroupClicked)
        viewModel.onEvent(GroupsEvent.GroupNameChanged("   "))
        viewModel.onEvent(GroupsEvent.CreateConfirmed)

        assertEquals(Res.string.validation_required, viewModel.state.value.nameError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun tooLongNameIsRejected() {
        viewModel.onEvent(GroupsEvent.NewGroupClicked)
        viewModel.onEvent(GroupsEvent.GroupNameChanged("a".repeat(MAX_GROUP_NAME_LENGTH + 1)))
        viewModel.onEvent(GroupsEvent.CreateConfirmed)

        assertEquals(Res.string.validation_name_too_long, viewModel.state.value.nameError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun failedCreateKeepsTheSheetOpenWithTheError() {
        repository.createResult = Result.failure(AppError.Network)
        viewModel.onEvent(GroupsEvent.NewGroupClicked)
        viewModel.onEvent(GroupsEvent.GroupNameChanged("Casa"))
        viewModel.onEvent(GroupsEvent.CreateConfirmed)

        assertEquals(GroupsSheet.Create, viewModel.state.value.sheet)
        assertEquals(AppError.Network, viewModel.state.value.submitError)
        assertNull(viewModel.state.value.navigateToGroup)
    }

    @Test
    fun joinSendsTrimmedCodeThenAsksToOpenTheGroup() {
        viewModel.onEvent(GroupsEvent.JoinClicked)
        viewModel.onEvent(GroupsEvent.CodeChanged(" ABC123 "))
        viewModel.onEvent(GroupsEvent.JoinConfirmed)

        assertEquals(listOf("joinGroup(ABC123)"), repository.calls)
        assertEquals("joined-id", viewModel.state.value.navigateToGroup)
    }

    @Test
    fun invalidInvitationShowsTheSpecificError() {
        repository.joinResult = Result.failure(AppError.BusinessRule("FZ011"))
        viewModel.onEvent(GroupsEvent.JoinClicked)
        viewModel.onEvent(GroupsEvent.CodeChanged("BAD"))
        viewModel.onEvent(GroupsEvent.JoinConfirmed)

        assertEquals(AppError.BusinessRule("FZ011"), viewModel.state.value.submitError)
        assertEquals(GroupsSheet.Join, viewModel.state.value.sheet)
    }

    @Test
    fun emptyCodeIsRejected() {
        viewModel.onEvent(GroupsEvent.JoinClicked)
        viewModel.onEvent(GroupsEvent.JoinConfirmed)

        assertEquals(Res.string.validation_required, viewModel.state.value.codeError)
        assertTrue(repository.calls.isEmpty())
    }
}
