package dev.lacruzjs.sobreio.ui.tags

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.TagUsage
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeTagRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_duplicate_name
import dev.lacruzjs.sobreio.generated.resources.tag_deleted
import dev.lacruzjs.sobreio.generated.resources.tag_reopened
import dev.lacruzjs.sobreio.generated.resources.tags_merged
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.generated.resources.validation_tag_merge_target
import dev.lacruzjs.sobreio.generated.resources.validation_tag_name_too_long
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import dev.lacruzjs.sobreio.ui.theme.TagColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagsViewModelTest : MainDispatcherTest() {

    private val trip = TagUsage("t1", "Trip", "#82B1D1", null, 12)
    private val home = TagUsage("t2", "home", null, null, 3)
    private val old = TagUsage("t3", "Old", "#A99FD6", "2026-11-01T10:00:00Z", 0)
    private val repository = FakeTagRepository(usage = listOf(trip, home, old))
    private val groups = FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Member)))

    private fun viewModel() = TagsViewModel("g1", repository, groups)

    @Test
    fun loadsLiveAndArchivedTagsSortedByName() {
        val s = viewModel().state.value

        assertFalse(s.isLoading)
        assertEquals(listOf("home", "Trip"), s.tags.map { it.name })
        assertEquals(listOf("Old"), s.archivedTags.map { it.name })
        assertEquals(12, s.tags.first { it.id == "t1" }.movements)
        assertEquals("Casa", s.group?.name)
        // Any member of a live group manages tags
        assertTrue(s.canEdit)
    }

    @Test
    fun aFailedLoadShowsTheErrorAndRetryRecovers() {
        repository.usageResult = Result.failure(AppError.Network)
        val vm = viewModel()
        assertEquals(AppError.Network, vm.state.value.error)

        repository.usageResult = null
        vm.onEvent(TagsEvent.Retry)
        assertNull(vm.state.value.error)
        assertEquals(2, vm.state.value.tags.size)
    }

    @Test
    fun anArchivedGroupIsReadOnly() {
        groups.groups = listOf(Group("g1", "Casa", "EUR", GroupRole.Owner, archivedAt = kotlin.time.Instant.parse("2026-11-01T10:00:00Z")))
        val vm = viewModel()
        assertFalse(vm.state.value.canEdit)

        vm.onEvent(TagsEvent.AddClicked)
        vm.onEvent(TagsEvent.DeleteClicked("t1"))
        assertEquals(TagsSheet.None, vm.state.value.sheet)
        assertTrue(repository.calls.isEmpty())
    }

    // Create and edit

    @Test
    fun createSendsTheNormalisedNameAndTheChosenColour() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.AddClicked)
        assertEquals(TagsSheet.Edit, vm.state.value.sheet)
        assertEquals(TagColor.Default, vm.state.value.color)
        vm.onEvent(TagsEvent.NameChanged("  New   tag "))
        vm.onEvent(TagsEvent.ColorSelected(TagColor.Rose))
        vm.onEvent(TagsEvent.SaveClicked)

        assertEquals(listOf("New tag"), repository.created)
        assertEquals(listOf<String?>(TagColor.Rose.hex), repository.createdColors)
        assertEquals(TagsSheet.None, vm.state.value.sheet)
    }

    @Test
    fun aBlankOrTooLongNameIsRejectedInTheSheet() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.AddClicked)
        vm.onEvent(TagsEvent.NameChanged("   "))
        vm.onEvent(TagsEvent.SaveClicked)
        assertEquals(Res.string.validation_required, vm.state.value.nameError)

        vm.onEvent(TagsEvent.NameChanged("x".repeat(51)))
        vm.onEvent(TagsEvent.SaveClicked)
        assertEquals(Res.string.validation_tag_name_too_long, vm.state.value.nameError)
        assertTrue(repository.created.isEmpty())
        assertEquals(TagsSheet.Edit, vm.state.value.sheet)
    }

    @Test
    fun aDuplicateNameShowsTheErrorUnderTheField() {
        repository.createResult = Result.failure(AppError.DuplicateName)
        val vm = viewModel()
        vm.onEvent(TagsEvent.AddClicked)
        vm.onEvent(TagsEvent.NameChanged("trip"))
        vm.onEvent(TagsEvent.SaveClicked)

        assertEquals(Res.string.error_duplicate_name, vm.state.value.nameError)
        assertEquals(TagsSheet.Edit, vm.state.value.sheet)
        assertNull(vm.state.value.submitError)
    }

    @Test
    fun editOpensTheSheetWithTheTagAndUpdatesIt() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.EditClicked("t1"))
        assertEquals("Trip", vm.state.value.name)
        assertEquals(TagColor.Sky, vm.state.value.color)

        vm.onEvent(TagsEvent.NameChanged("Trips"))
        vm.onEvent(TagsEvent.SaveClicked)

        assertTrue("updateTag(t1,Trips,${TagColor.Sky.hex})" in repository.calls)
        assertEquals(TagsSheet.None, vm.state.value.sheet)
    }

    @Test
    fun editingATagWithNoColourStartsOnTheDefault() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.EditClicked("t2"))

        assertEquals(TagColor.Default, vm.state.value.color)
    }

    // Delete and reopen

    @Test
    fun deleteArchivesAtOnceHidesTheRowAndOffersUndo() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.DeleteClicked("t1"))

        assertTrue("archiveTag(t1)" in repository.calls)
        assertEquals(listOf("home"), vm.state.value.tags.map { it.name })
        assertTrue(vm.state.value.archivedTags.any { it.id == "t1" })
        assertEquals(Res.string.tag_deleted, vm.state.value.undoMessage?.message)
        assertEquals("Trip", vm.state.value.undoMessage?.argument)
    }

    @Test
    fun undoUnarchivesAndBringsTheRowBack() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.DeleteClicked("t1"))
        vm.onEvent(TagsEvent.UndoClicked)

        assertTrue("unarchiveTag(t1)" in repository.calls)
        assertEquals(listOf("home", "Trip"), vm.state.value.tags.map { it.name })
        assertNull(vm.state.value.undoMessage)
    }

    @Test
    fun aFailedArchiveBringsTheRowBackWithTheError() {
        repository.archiveResult = Result.failure(AppError.Network)
        val vm = viewModel()
        vm.onEvent(TagsEvent.DeleteClicked("t1"))

        assertEquals(listOf("home", "Trip"), vm.state.value.tags.map { it.name })
        assertEquals(AppError.Network, vm.state.value.actionError)
        assertNull(vm.state.value.undoMessage)
    }

    @Test
    fun reopenMovesTheTagToTheLiveListAndUndoArchivesItAgain() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.ReopenClicked("t3"))

        assertTrue("unarchiveTag(t3)" in repository.calls)
        assertTrue(vm.state.value.tags.any { it.id == "t3" })
        assertTrue(vm.state.value.archivedTags.isEmpty())
        assertEquals(Res.string.tag_reopened, vm.state.value.undoMessage?.message)

        vm.onEvent(TagsEvent.UndoClicked)
        assertTrue("archiveTag(t3)" in repository.calls)
    }

    @Test
    fun reopeningIntoAClashShowsTheDuplicateNameError() {
        repository.unarchiveResult = Result.failure(AppError.DuplicateName)
        val vm = viewModel()
        vm.onEvent(TagsEvent.ReopenClicked("t3"))

        assertEquals(AppError.DuplicateName, vm.state.value.actionError)
        assertEquals(listOf("Old"), vm.state.value.archivedTags.map { it.name })
    }

    // Merge

    @Test
    fun mergeNeedsAnotherLiveTag() {
        repository.usage = listOf(trip)
        val vm = viewModel()
        assertFalse(vm.state.value.canMerge)

        vm.onEvent(TagsEvent.MergeClicked("t1"))
        assertEquals(TagsSheet.None, vm.state.value.sheet)
    }

    @Test
    fun theMergeSheetOffersTheOtherLiveTags() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.MergeClicked("t1"))

        assertEquals(TagsSheet.Merge, vm.state.value.sheet)
        assertEquals(listOf("home"), vm.state.value.mergeCandidates.map { it.name })
    }

    @Test
    fun mergingWithoutATargetShowsAnError() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.MergeClicked("t1"))
        vm.onEvent(TagsEvent.MergeConfirmed)

        assertEquals(Res.string.validation_tag_merge_target, vm.state.value.mergeTargetError)
        assertTrue(repository.calls.none { it.startsWith("mergeTags") })
    }

    @Test
    fun mergeCallsTheDatabaseClosesTheSheetAndShowsAPlainNotice() {
        val vm = viewModel()
        vm.onEvent(TagsEvent.MergeClicked("t1"))
        vm.onEvent(TagsEvent.MergeTargetSelected("t2"))
        vm.onEvent(TagsEvent.MergeConfirmed)

        assertTrue("mergeTags(t1,t2)" in repository.calls)
        assertEquals(TagsSheet.None, vm.state.value.sheet)
        assertEquals(Res.string.tags_merged, vm.state.value.undoMessage?.message)
        // Merging cannot be undone
        assertFalse(vm.state.value.undoMessage?.undoable ?: true)
    }

    @Test
    fun aRefusedMergeKeepsTheSheetOpenWithTheError() {
        repository.mergeResult = Result.failure(AppError.BusinessRule("FZ022"))
        val vm = viewModel()
        vm.onEvent(TagsEvent.MergeClicked("t1"))
        vm.onEvent(TagsEvent.MergeTargetSelected("t2"))
        vm.onEvent(TagsEvent.MergeConfirmed)

        assertEquals(TagsSheet.Merge, vm.state.value.sheet)
        assertEquals(AppError.BusinessRule("FZ022"), vm.state.value.submitError)
        assertNull(vm.state.value.undoMessage)
    }
}
