package dev.lacruzjs.sobreio.ui.group

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeLastGroupStore
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupViewModelTest : MainDispatcherTest() {

    private val repository = FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Owner)))
    private val lastGroup = FakeLastGroupStore()
    private val auth = FakeAuthRepository(AuthState.SignedIn(User("u1", "ana@example.com", "Ana")))

    private fun viewModel(groupId: String) = GroupViewModel(groupId, repository, lastGroup, auth)

    @Test
    fun loadsTheGroupAndRemembersItAsLast() {
        val state = viewModel("g1").state.value

        assertEquals(Group("g1", "Casa", "EUR", GroupRole.Owner), state.group)
        assertFalse(state.isLoading)
        assertEquals("g1", lastGroup.get("u1"))
    }

    @Test
    fun missingGroupClearsTheLastGroupAndAsksToGoBack() {
        lastGroup.set("u1", "gone")
        val viewModel = viewModel("gone")

        assertTrue(viewModel.state.value.navigateBack)
        assertNull(lastGroup.get("u1"))

        viewModel.onEvent(GroupEvent.NavigationHandled)
        assertFalse(viewModel.state.value.navigateBack)
    }

    @Test
    fun networkFailureKeepsTheStoredGroupAndShowsTheError() {
        lastGroup.set("u1", "g1")
        repository.groups = emptyList()
        // A group that cannot be fetched because of the network must not be forgotten
        val failing = object : dev.lacruzjs.sobreio.data.repository.GroupRepository by repository {
            override suspend fun getGroup(id: String): Result<Group> = Result.failure(AppError.Network)
        }
        val viewModel = GroupViewModel("g1", failing, lastGroup, auth)

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.navigateBack)
        assertEquals("g1", lastGroup.get("u1"))
    }

    @Test
    fun refreshReadsTheGroupAgainWithoutTheSpinner() {
        val viewModel = viewModel("g1")
        repository.groups = listOf(Group("g1", "Hogar", "EUR", GroupRole.Owner, color = "#3B6E8F"))
        viewModel.onEvent(GroupEvent.Refresh)

        assertEquals("Hogar", viewModel.state.value.group?.name)
        assertEquals("#3B6E8F", viewModel.state.value.group?.color)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun refreshOfAGroupThatIsGoneAsksToGoBack() {
        val viewModel = viewModel("g1")
        repository.groups = emptyList()
        viewModel.onEvent(GroupEvent.Refresh)

        assertTrue(viewModel.state.value.navigateBack)
        assertNull(lastGroup.get("u1"))
    }

    @Test
    fun anArchivedGroupLoadsButCannotBeEdited() {
        repository.groups = listOf(
            Group("g1", "Casa", "EUR", GroupRole.Owner, archivedAt = kotlin.time.Instant.parse("2026-11-01T10:00:00Z")),
        )
        val group = viewModel("g1").state.value.group

        assertTrue(group?.isArchived == true)
        assertFalse(group?.canEdit == true)
    }

    @Test
    fun newItemSheetOpensAndCloses() {
        val viewModel = viewModel("g1")
        viewModel.onEvent(GroupEvent.NewItemClicked)
        assertTrue(viewModel.state.value.newItemSheetOpen)
        viewModel.onEvent(GroupEvent.NewItemDismissed)
        assertFalse(viewModel.state.value.newItemSheetOpen)
    }
}
