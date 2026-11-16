package dev.lacruzjs.sobreio.ui.deleteaccount

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem
import dev.lacruzjs.sobreio.fake.FakeAccountDeletionRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteAccountViewModelTest : MainDispatcherTest() {

    private val repository = FakeAccountDeletionRepository()
    private fun item(id: String, outcome: DeletionOutcome) = DeletionPreviewItem(id, "G$id", null, null, outcome)
    private fun viewModel() = DeleteAccountViewModel(repository)

    @Test
    fun loadsThePreviewAndSplitsItByOutcome() {
        repository.preview = Result.success(
            listOf(item("1", DeletionOutcome.Blocked), item("2", DeletionOutcome.Delete), item("3", DeletionOutcome.Leave)),
        )
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals(listOf("1"), state.blocked.map { it.groupId })
        assertEquals(listOf("2"), state.deleted.map { it.groupId })
        assertEquals(listOf("3"), state.left.map { it.groupId })
    }

    @Test
    fun buttonNeedsTheSwitchAndALoadedPreview() {
        repository.preview = Result.success(listOf(item("2", DeletionOutcome.Leave)))
        val vm = viewModel()
        assertFalse(vm.state.value.canDelete)

        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))
        assertTrue(vm.state.value.canDelete)

        vm.onEvent(DeleteAccountEvent.ConfirmChanged(false))
        assertFalse(vm.state.value.canDelete)
    }

    @Test
    fun aBlockedGroupDisablesTheButtonEvenWithTheSwitchOn() {
        repository.preview = Result.success(listOf(item("1", DeletionOutcome.Blocked)))
        val vm = viewModel()
        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))
        vm.onEvent(DeleteAccountEvent.DeleteClicked)

        assertFalse(vm.state.value.canDelete)
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun failedPreviewShowsTheErrorAndKeepsDeleteOffUntilRetrySucceeds() {
        repository.preview = Result.failure(AppError.Network)
        val vm = viewModel()
        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))

        assertEquals(AppError.Network, vm.state.value.loadError)
        assertNull(vm.state.value.items)
        assertFalse(vm.state.value.canDelete)

        repository.preview = Result.success(emptyList())
        vm.onEvent(DeleteAccountEvent.Retry)
        assertNull(vm.state.value.loadError)
        assertTrue(vm.state.value.canDelete)
    }

    @Test
    fun deletingCallsTheRepositoryOnce() {
        val vm = viewModel()
        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))
        vm.onEvent(DeleteAccountEvent.DeleteClicked)

        assertEquals(1, repository.deleteCalls)
        assertFalse(vm.state.value.isDeleting)
        assertNull(vm.state.value.deleteError)
    }

    @Test
    fun businessRuleFailureIsShownAndThePreviewIsReadAgain() {
        repository.deleteResult = Result.failure(AppError.BusinessRule("FZ022:delete-account"))
        val vm = viewModel()
        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))
        val before = repository.previewCalls
        vm.onEvent(DeleteAccountEvent.DeleteClicked)

        assertEquals(AppError.BusinessRule("FZ022:delete-account"), vm.state.value.deleteError)
        assertEquals(before + 1, repository.previewCalls)
        assertFalse(vm.state.value.isDeleting)
    }

    @Test
    fun refreshReadsAgainWithoutLosingTheSwitch() {
        val vm = viewModel()
        vm.onEvent(DeleteAccountEvent.ConfirmChanged(true))
        repository.preview = Result.success(listOf(item("1", DeletionOutcome.Blocked)))
        vm.onEvent(DeleteAccountEvent.Refresh)

        assertTrue(vm.state.value.confirmed)
        assertEquals(1, vm.state.value.blocked.size)
    }
}
