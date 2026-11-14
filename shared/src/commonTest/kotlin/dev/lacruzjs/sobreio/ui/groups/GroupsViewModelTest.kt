package dev.lacruzjs.sobreio.ui.groups

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeLastGroupStore
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_deleted
import dev.lacruzjs.sobreio.generated.resources.group_left
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupsViewModelTest : MainDispatcherTest() {

    private val casa = Group("1", "Casa", "EUR", GroupRole.Owner)
    private val piso = Group("2", "Piso", "EUR", GroupRole.Member)
    private val repository = FakeGroupRepository(listOf(casa, piso))
    private val auth = FakeAuthRepository(AuthState.SignedIn(User("u1", "ana@example.com", "Ana")))
    private val lastGroup = FakeLastGroupStore()
    private val viewModel by lazy { GroupsViewModel(repository, auth, lastGroup) }

    private fun loaded(): GroupsViewModel = viewModel.also { it.onEvent(GroupsEvent.Refresh) }

    @Test
    fun refreshLoadsTheGroups() {
        viewModel.onEvent(GroupsEvent.Refresh)

        assertEquals(listOf(casa, piso), viewModel.state.value.groups)
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

    @Test
    fun signOutCallsTheRepository() {
        viewModel.onEvent(GroupsEvent.SignOutClicked)

        assertEquals(listOf("signOut()"), auth.calls)
        assertFalse(viewModel.state.value.isSigningOut)
        assertNull(viewModel.state.value.signOutError)
    }

    @Test
    fun failedSignOutShowsTheError() {
        auth.result = Result.failure(AppError.Network)
        viewModel.onEvent(GroupsEvent.SignOutClicked)

        assertEquals(AppError.Network, viewModel.state.value.signOutError)
        assertFalse(viewModel.state.value.isSigningOut)
    }

    @Test
    fun editOpensTheSheetWithTheNameAndRenameUpdatesTheList() {
        loaded()
        viewModel.onEvent(GroupsEvent.EditClicked("1"))
        assertEquals(GroupsSheet.Edit, viewModel.state.value.sheet)
        assertEquals("Casa", viewModel.state.value.name)

        viewModel.onEvent(GroupsEvent.GroupNameChanged("  Hogar "))
        viewModel.onEvent(GroupsEvent.EditConfirmed)

        assertTrue("renameGroup(1, Hogar)" in repository.calls)
        assertEquals(GroupsSheet.None, viewModel.state.value.sheet)
        assertEquals("Hogar", viewModel.state.value.groups.first { it.id == "1" }.name)
    }

    @Test
    fun aMemberCannotOpenTheEditSheet() {
        loaded()
        viewModel.onEvent(GroupsEvent.EditClicked("2"))

        assertEquals(GroupsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun failedRenameKeepsTheSheetOpenWithTheError() {
        repository.renameResult = Result.failure(AppError.BusinessRule("FZ016"))
        loaded()
        viewModel.onEvent(GroupsEvent.EditClicked("1"))
        viewModel.onEvent(GroupsEvent.GroupNameChanged("Hogar"))
        viewModel.onEvent(GroupsEvent.EditConfirmed)

        assertEquals(GroupsSheet.Edit, viewModel.state.value.sheet)
        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertEquals("Casa", viewModel.state.value.groups.first { it.id == "1" }.name)
    }

    @Test
    fun ownerDeleteRequestsTheDeletionAtOnceHidesTheGroupAndOffersUndo() {
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))

        assertTrue("requestGroupDeletion(1)" in repository.calls)
        assertEquals(listOf(piso), viewModel.state.value.groups)
        assertEquals(Res.string.group_deleted, viewModel.state.value.undoMessage?.message)
        assertEquals("Casa", viewModel.state.value.undoMessage?.argument)
    }

    @Test
    fun undoingAnOwnerDeleteCancelsItAndBringsTheGroupBack() {
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))
        viewModel.onEvent(GroupsEvent.UndoClicked)

        assertTrue("cancelGroupDeletion(1)" in repository.calls)
        assertEquals(listOf(casa, piso), viewModel.state.value.groups)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun ownerDeleteThatFailsPutsTheGroupBackWithTheError() {
        repository.deleteResult = Result.failure(AppError.Network)
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))

        assertEquals(listOf(casa, piso), viewModel.state.value.groups)
        assertEquals(AppError.Network, viewModel.state.value.actionError)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun ownerDeleteForgetsTheLastGroupWhenTheSnackbarEnds() {
        lastGroup.set("u1", "1")
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))
        assertEquals("1", lastGroup.get("u1"))

        viewModel.onEvent(GroupsEvent.UndoExpired)

        assertNull(lastGroup.get("u1"))
        assertFalse("cancelGroupDeletion(1)" in repository.calls)
    }

    @Test
    fun leavingHidesTheGroupButSendsNothingUntilTheSnackbarEnds() {
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("2"))

        assertEquals(listOf(casa), viewModel.state.value.groups)
        assertEquals(Res.string.group_left, viewModel.state.value.undoMessage?.message)
        assertFalse("leaveGroup(2)" in repository.calls)
    }

    @Test
    fun undoingALeaveMakesNoNetworkCall() {
        loaded()
        repository.calls.clear()
        viewModel.onEvent(GroupsEvent.DeleteClicked("2"))
        viewModel.onEvent(GroupsEvent.UndoClicked)

        assertTrue(repository.calls.isEmpty())
        assertEquals(listOf(casa, piso), viewModel.state.value.groups)
    }

    @Test
    fun leaveIsSentAndTheLastGroupClearedWhenTheSnackbarTimesOut() {
        lastGroup.set("u1", "2")
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("2"))
        viewModel.onEvent(GroupsEvent.UndoExpired)

        assertTrue("leaveGroup(2)" in repository.calls)
        assertNull(lastGroup.get("u1"))
        assertEquals(listOf(casa), viewModel.state.value.groups)
    }

    @Test
    fun startingAnotherDestructiveActionCommitsThePendingLeave() {
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("2"))
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))

        assertTrue("leaveGroup(2)" in repository.calls)
        assertTrue("requestGroupDeletion(1)" in repository.calls)
    }

    @Test
    fun onlyOwnerLastLeaveShowsTheSpecificErrorAndPutsTheGroupBack() {
        repository.leaveResult = Result.failure(AppError.BusinessRule("FZ009"))
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("2"))
        viewModel.onEvent(GroupsEvent.UndoExpired)

        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.actionError)
        assertEquals(listOf(casa, piso), viewModel.state.value.groups)
    }

    @Test
    fun failedUndoOfAnOwnerDeleteShowsTheErrorAndKeepsTheGroupHidden() {
        repository.cancelDeletionResult = Result.failure(AppError.Network)
        loaded()
        viewModel.onEvent(GroupsEvent.DeleteClicked("1"))
        viewModel.onEvent(GroupsEvent.UndoClicked)

        assertEquals(AppError.Network, viewModel.state.value.actionError)
        assertEquals(listOf(piso), viewModel.state.value.groups)
    }
}
