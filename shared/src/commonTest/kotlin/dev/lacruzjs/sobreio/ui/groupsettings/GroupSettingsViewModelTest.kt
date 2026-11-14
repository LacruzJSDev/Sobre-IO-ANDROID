package dev.lacruzjs.sobreio.ui.groupsettings

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeLastGroupStore
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_archived_message
import dev.lacruzjs.sobreio.generated.resources.group_deleted
import dev.lacruzjs.sobreio.generated.resources.group_left
import dev.lacruzjs.sobreio.generated.resources.invitation_revoked
import dev.lacruzjs.sobreio.generated.resources.member_removed
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import dev.lacruzjs.sobreio.ui.groups.GroupFormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupSettingsViewModelTest : MainDispatcherTest() {

    private val ana = Member("u1", "Ana", GroupRole.Owner)
    private val marta = Member("u2", "Marta", GroupRole.Admin)
    private val luis = Member("u3", "Luis", GroupRole.Member)
    private val invitation = Invitation("i1", "AAAA11112222", GroupRole.Member, Instant.parse("2026-11-20T10:00:00Z"))
    private val expired = Invitation("i2", "BBBB33334444", GroupRole.Admin, Instant.parse("2026-11-01T10:00:00Z"))

    private val groupRepository = FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Owner)))
    private val memberRepository = FakeMemberRepository(listOf(ana, marta, luis))
    private val auth = FakeAuthRepository(AuthState.SignedIn(User("u1", "ana@example.com", "Ana")))
    private val lastGroup = FakeLastGroupStore()

    private fun viewModel(role: GroupRole = GroupRole.Owner): GroupSettingsViewModel {
        groupRepository.groups = groupRepository.groups.map { it.copy(role = role) }
        groupRepository.invitations = listOf(invitation, expired)
        return GroupSettingsViewModel("g1", groupRepository, memberRepository, auth, lastGroup)
    }

    private fun GroupSettingsViewModel.change(change: GroupFormState.() -> GroupFormState) =
        onEvent(GroupSettingsEvent.FormChanged(state.value.form.change()))

    // Loading and permissions

    @Test
    fun anOwnerSeesTheDetailsMembersAndPendingInvitations() {
        val state = viewModel().state.value

        assertEquals("Casa", state.group?.name)
        assertEquals("u1", state.currentUserId)
        assertEquals(listOf(ana, marta, luis), state.members)
        assertEquals(listOf(invitation, expired), state.invitations)
        assertTrue(state.hasSeveralMembers)
        assertTrue(state.canEditDetails && state.canInvite && state.canManageMembers && state.canArchive)
        assertFalse(state.isLoading)
    }

    @Test
    fun aSingleMemberGroupDoesNotHaveSeveralMembers() {
        memberRepository.members = listOf(ana)

        assertFalse(viewModel().state.value.hasSeveralMembers)
    }

    @Test
    fun anAdminEditsAndInvitesButDoesNotManageMembersOrArchive() {
        val viewModel = viewModel(GroupRole.Admin)
        val state = viewModel.state.value

        assertTrue(state.canEditDetails && state.canInvite)
        assertFalse(state.canManageMembers)
        assertFalse(state.canArchive)

        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u3"))
        viewModel.onEvent(GroupSettingsEvent.RemoveMemberClicked("u3"))
        viewModel.onEvent(GroupSettingsEvent.ArchiveClicked)
        viewModel.onEvent(GroupSettingsEvent.DeleteClicked)

        assertEquals(GroupSettingsSheet.None, viewModel.state.value.sheet)
        assertEquals(3, viewModel.state.value.members.size)
        assertTrue(groupRepository.calls.none { it.startsWith("archiveGroup") || it.startsWith("requestGroupDeletion") })
    }

    @Test
    fun aMemberReadsDetailsAndMembersOnlyAndLoadsNoInvitations() {
        val viewModel = viewModel(GroupRole.Member)
        val state = viewModel.state.value

        assertFalse(state.canEditDetails || state.canInvite || state.canManageMembers)
        assertTrue(state.invitations.isEmpty())
        assertTrue(groupRepository.calls.none { it.startsWith("getPendingInvitations") })

        viewModel.onEvent(GroupSettingsEvent.EditClicked)
        viewModel.onEvent(GroupSettingsEvent.InviteClicked)
        assertEquals(GroupSettingsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun aGroupThatIsGoneAsksToGoBackToGroups() {
        groupRepository.groups = emptyList()
        val viewModel = GroupSettingsViewModel("g1", groupRepository, memberRepository, auth, lastGroup)

        assertTrue(viewModel.state.value.navigateToGroups)
        viewModel.onEvent(GroupSettingsEvent.NavigationHandled)
        assertFalse(viewModel.state.value.navigateToGroups)
    }

    @Test
    fun aFailedLoadShowsTheErrorAndRetryLoadsAgain() {
        groupRepository.groupsResult = null
        val failing = object : dev.lacruzjs.sobreio.data.repository.GroupRepository by groupRepository {
            override suspend fun getGroup(id: String): Result<Group> = Result.failure(AppError.Network)
        }
        val viewModel = GroupSettingsViewModel("g1", failing, memberRepository, auth, lastGroup)

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.navigateToGroups)
    }

    // Details

    @Test
    fun editSendsEveryFieldAndUpdatesTheDetails() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.EditClicked)
        assertEquals(GroupSettingsSheet.Edit, viewModel.state.value.sheet)
        assertEquals("Casa", viewModel.state.value.form.name)

        viewModel.change {
            copy(name = " Hogar ", color = "#8E3B4A", icon = "key", periodStartDay = 15, timezone = "Europe/Lisbon")
        }
        viewModel.onEvent(GroupSettingsEvent.EditConfirmed)

        assertTrue("updateGroup(g1, Hogar, Europe/Lisbon, 15, #8E3B4A, key)" in groupRepository.calls)
        val group = viewModel.state.value.group
        assertEquals("Hogar", group?.name)
        assertEquals(15, group?.periodStartDay)
        assertEquals("Europe/Lisbon", group?.timezone)
        assertEquals("#8E3B4A", group?.color)
        assertEquals("key", group?.icon)
        assertEquals(GroupSettingsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun editRejectsABlankNameAndKeepsTheSheetOpenOnFailure() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.EditClicked)
        viewModel.change { copy(name = "  ") }
        viewModel.onEvent(GroupSettingsEvent.EditConfirmed)
        assertEquals(Res.string.validation_required, viewModel.state.value.form.nameError)
        assertTrue(groupRepository.calls.none { it.startsWith("updateGroup") })

        groupRepository.updateResult = Result.failure(AppError.Network)
        viewModel.change { copy(name = "Hogar") }
        viewModel.onEvent(GroupSettingsEvent.EditConfirmed)
        assertEquals(GroupSettingsSheet.Edit, viewModel.state.value.sheet)
        assertEquals(AppError.Network, viewModel.state.value.submitError)
        assertEquals("Casa", viewModel.state.value.group?.name)
    }

    // Members

    @Test
    fun anOwnerChangesAnotherMembersRole() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u3"))
        assertEquals(GroupSettingsSheet.Role, viewModel.state.value.sheet)
        assertEquals(luis, viewModel.state.value.roleMember)

        viewModel.onEvent(GroupSettingsEvent.RoleSelected(GroupRole.Owner))

        assertEquals(listOf("changeRole(g1, u3, Owner)"), memberRepository.calls)
        assertEquals(GroupRole.Owner, viewModel.state.value.members.first { it.userId == "u3" }.role)
        assertEquals(GroupSettingsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun theLastOwnerErrorStaysInTheRoleSheet() {
        memberRepository.changeRoleResult = Result.failure(AppError.BusinessRule("FZ009"))
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u2"))
        viewModel.onEvent(GroupSettingsEvent.RoleSelected(GroupRole.Member))

        assertEquals(GroupSettingsSheet.Role, viewModel.state.value.sheet)
        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.submitError)
        assertEquals(GroupRole.Admin, viewModel.state.value.members.first { it.userId == "u2" }.role)
    }

    @Test
    fun selectingTheCurrentRoleJustClosesTheSheet() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u2"))
        viewModel.onEvent(GroupSettingsEvent.RoleSelected(GroupRole.Admin))

        assertTrue(memberRepository.calls.isEmpty())
        assertEquals(GroupSettingsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun anOwnerCanChangeTheirOwnRoleButNotRemoveThemselves() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RemoveMemberClicked("u1"))
        assertEquals(3, viewModel.state.value.members.size)
        assertNull(viewModel.state.value.undoMessage)

        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u1"))
        assertEquals(GroupSettingsSheet.Role, viewModel.state.value.sheet)
        viewModel.onEvent(GroupSettingsEvent.RoleSelected(GroupRole.Admin))

        assertEquals(listOf("changeRole(g1, u1, Admin)"), memberRepository.calls)
        assertEquals(GroupRole.Admin, viewModel.state.value.members.first { it.userId == "u1" }.role)
    }

    @Test
    fun demotingTheLastOwnerShowsTheErrorInTheSheet() {
        memberRepository.changeRoleResult = Result.failure(AppError.BusinessRule("FZ009"))
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RoleClicked("u1"))
        viewModel.onEvent(GroupSettingsEvent.RoleSelected(GroupRole.Admin))

        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.submitError)
        assertEquals(GroupSettingsSheet.Role, viewModel.state.value.sheet)
        assertEquals(GroupRole.Owner, viewModel.state.value.members.first { it.userId == "u1" }.role)
    }

    @Test
    fun removingHidesTheMemberAtOnceAndWritesOnlyWhenTheSnackbarEnds() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RemoveMemberClicked("u3"))

        assertEquals(listOf(ana, marta), viewModel.state.value.members)
        assertEquals(Res.string.member_removed, viewModel.state.value.undoMessage?.message)
        assertEquals("Luis", viewModel.state.value.undoMessage?.argument)
        assertTrue(memberRepository.calls.isEmpty())

        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertEquals(listOf("removeMember(g1, u3)"), memberRepository.calls)
        assertEquals(listOf(ana, marta), viewModel.state.value.members)
    }

    @Test
    fun undoingARemovalMakesNoCallAndBringsTheMemberBackInPlace() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RemoveMemberClicked("u2"))
        viewModel.onEvent(GroupSettingsEvent.UndoClicked)

        assertTrue(memberRepository.calls.isEmpty())
        assertEquals(listOf(ana, marta, luis), viewModel.state.value.members)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun aFailedRemovalPutsTheMemberBackWithTheError() {
        memberRepository.removeResult = Result.failure(AppError.BusinessRule("FZ009"))
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RemoveMemberClicked("u2"))
        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertEquals(listOf(ana, marta, luis), viewModel.state.value.members)
        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.actionError)
    }

    // Invitations

    @Test
    fun invitingCreatesTheInvitationWithTheChosenRoleAndShowsItsCode() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.InviteClicked)
        assertEquals(GroupRole.Member, viewModel.state.value.inviteRole)

        viewModel.onEvent(GroupSettingsEvent.InviteRoleSelected(GroupRole.Admin))
        viewModel.onEvent(GroupSettingsEvent.InviteConfirmed)

        assertTrue("createInvitation(g1, Admin)" in groupRepository.calls)
        val created = viewModel.state.value.createdInvitation
        assertEquals("ABC123DEF456", created?.code)
        assertEquals(GroupRole.Admin, created?.role)
        assertEquals(created, viewModel.state.value.invitations.first())
        assertEquals(3, viewModel.state.value.invitations.size)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun anInvitationIsNeverForAnOwner() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.InviteClicked)
        viewModel.onEvent(GroupSettingsEvent.InviteRoleSelected(GroupRole.Owner))

        assertEquals(GroupRole.Member, viewModel.state.value.inviteRole)
    }

    @Test
    fun aFailedInvitationShowsTheErrorInTheSheet() {
        groupRepository.invitationResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = viewModel(GroupRole.Admin)
        viewModel.onEvent(GroupSettingsEvent.InviteClicked)
        viewModel.onEvent(GroupSettingsEvent.InviteConfirmed)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.submitError)
        assertNull(viewModel.state.value.createdInvitation)
    }

    @Test
    fun anExpiredInvitationIsFlaggedByItsDate() {
        assertTrue(expired.isExpired(Instant.parse("2026-11-14T10:00:00Z")))
        assertFalse(invitation.isExpired(Instant.parse("2026-11-14T10:00:00Z")))
    }

    @Test
    fun revokingHidesTheInvitationAtOnceAndRevokesOnlyWhenTheSnackbarEnds() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RevokeInvitationClicked("i1"))

        assertEquals(listOf(expired), viewModel.state.value.invitations)
        assertEquals(Res.string.invitation_revoked, viewModel.state.value.undoMessage?.message)
        assertEquals("AAAA11112222", viewModel.state.value.undoMessage?.argument)
        assertTrue(groupRepository.calls.none { it.startsWith("revokeInvitation") })

        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertTrue("revokeInvitation(i1)" in groupRepository.calls)
    }

    @Test
    fun undoingARevocationBringsTheInvitationBackAndSendsNothing() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RevokeInvitationClicked("i1"))
        viewModel.onEvent(GroupSettingsEvent.UndoClicked)

        assertEquals(listOf(invitation, expired), viewModel.state.value.invitations)
        assertTrue(groupRepository.calls.none { it.startsWith("revokeInvitation") })
    }

    @Test
    fun aFailedRevocationPutsTheInvitationBackWithTheError() {
        groupRepository.revokeResult = Result.failure(AppError.BusinessRule("FZ011"))
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.RevokeInvitationClicked("i1"))
        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertEquals(listOf(invitation, expired), viewModel.state.value.invitations)
        assertEquals(AppError.BusinessRule("FZ011"), viewModel.state.value.actionError)
    }

    // Archive, reopen, delete, leave

    @Test
    fun archivingMakesTheGroupReadOnlyAndOffersUndo() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.ArchiveClicked)

        assertTrue("archiveGroup(g1)" in groupRepository.calls)
        val state = viewModel.state.value
        assertTrue(state.group?.isArchived == true)
        assertFalse(state.canEditDetails)
        assertTrue(state.canReopen)
        assertEquals(Res.string.group_archived_message, state.undoMessage?.message)
        assertTrue(state.invitations.isEmpty())

        viewModel.onEvent(GroupSettingsEvent.UndoClicked)

        assertTrue("reopenGroup(g1)" in groupRepository.calls)
        assertTrue(viewModel.state.value.canArchive)
        assertEquals(listOf(invitation, expired), viewModel.state.value.invitations)
    }

    @Test
    fun reopeningAnArchivedGroupMakesItEditableAgain() {
        groupRepository.groups = groupRepository.groups.map { it.copy(archivedAt = Instant.parse("2026-11-01T10:00:00Z")) }
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.canReopen)

        viewModel.onEvent(GroupSettingsEvent.ReopenClicked)

        assertTrue("reopenGroup(g1)" in groupRepository.calls)
        assertTrue(viewModel.state.value.canEditDetails)
        assertFalse(viewModel.state.value.group?.isArchived == true)
    }

    @Test
    fun aFailedArchiveShowsTheError() {
        groupRepository.archiveResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.ArchiveClicked)

        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.actionError)
        assertFalse(viewModel.state.value.isWorking)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun deletingRequestsThePurgeAtOnceAndGoesBackWhenTheSnackbarEnds() {
        lastGroup.set("u1", "g1")
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.DeleteClicked)

        assertTrue("requestGroupDeletion(g1)" in groupRepository.calls)
        assertEquals(Res.string.group_deleted, viewModel.state.value.undoMessage?.message)
        assertTrue(viewModel.state.value.isWorking)
        assertFalse(viewModel.state.value.navigateToGroups)

        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertTrue(viewModel.state.value.navigateToGroups)
        assertNull(lastGroup.get("u1"))
    }

    @Test
    fun undoingADeletionCancelsItAndStaysOnTheScreen() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.DeleteClicked)
        viewModel.onEvent(GroupSettingsEvent.UndoClicked)

        assertTrue("cancelGroupDeletion(g1)" in groupRepository.calls)
        assertFalse(viewModel.state.value.isWorking)
        assertFalse(viewModel.state.value.navigateToGroups)
    }

    @Test
    fun aNonOwnerLeavesOnlyWhenTheSnackbarEnds() {
        lastGroup.set("u1", "g1")
        val viewModel = viewModel(GroupRole.Member)
        viewModel.onEvent(GroupSettingsEvent.LeaveClicked)

        assertEquals(Res.string.group_left, viewModel.state.value.undoMessage?.message)
        assertTrue(groupRepository.calls.none { it.startsWith("leaveGroup") })
        assertFalse(viewModel.state.value.navigateToGroups)

        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertTrue("leaveGroup(g1)" in groupRepository.calls)
        assertTrue(viewModel.state.value.navigateToGroups)
        assertNull(lastGroup.get("u1"))
    }

    @Test
    fun undoingALeaveSendsNothing() {
        val viewModel = viewModel(GroupRole.Admin)
        viewModel.onEvent(GroupSettingsEvent.LeaveClicked)
        viewModel.onEvent(GroupSettingsEvent.UndoClicked)

        assertTrue(groupRepository.calls.none { it.startsWith("leaveGroup") })
        assertFalse(viewModel.state.value.isWorking)
    }

    @Test
    fun aFailedLeaveStaysOnTheScreenWithTheError() {
        groupRepository.leaveResult = Result.failure(AppError.BusinessRule("FZ009"))
        val viewModel = viewModel(GroupRole.Admin)
        viewModel.onEvent(GroupSettingsEvent.LeaveClicked)
        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.actionError)
        assertFalse(viewModel.state.value.isWorking)
        assertFalse(viewModel.state.value.navigateToGroups)
    }

    @Test
    fun anOwnerLeavesWhenTheSnackbarEndsAndTheLastOwnerErrorRestoresTheScreen() {
        val viewModel = viewModel()
        viewModel.onEvent(GroupSettingsEvent.LeaveClicked)
        assertEquals(Res.string.group_left, viewModel.state.value.undoMessage?.message)
        assertTrue(viewModel.state.value.isWorking)

        groupRepository.leaveResult = Result.failure(AppError.BusinessRule("FZ009"))
        viewModel.onEvent(GroupSettingsEvent.UndoExpired)

        assertTrue("leaveGroup(g1)" in groupRepository.calls)
        assertEquals(AppError.BusinessRule("FZ009"), viewModel.state.value.actionError)
        assertFalse(viewModel.state.value.isWorking)
        assertFalse(viewModel.state.value.navigateToGroups)
    }

    @Test
    fun aNonOwnerCannotDelete() {
        val member = viewModel(GroupRole.Member)
        member.onEvent(GroupSettingsEvent.DeleteClicked)
        assertTrue(groupRepository.calls.none { it.startsWith("requestGroupDeletion") })
    }
}
