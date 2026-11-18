package dev.lacruzjs.sobreio.ui.envelopetransfers

import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeTransferRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.transfer_deleted
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class EnvelopeTransfersViewModelTest : MainDispatcherTest() {

    private val unassigned = Envelope("u", "g1", "Sin asignar", EnvelopeKind.System, 0, 0)
    private val food = Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 40_000, 0)
    private val groups = FakeGroupRepository(listOf(Group("g1", "Home", "EUR", GroupRole.Member)))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food))
    private val transfers = FakeEnvelopeTransferRepository()

    private fun transfer(index: Int, origin: TransferOrigin = TransferOrigin.Manual) = EnvelopeTransfer(
        "t$index", "u", "e1", 1_000L * index, origin, Instant.parse("2026-11-${10 + index}T10:00:00Z"),
    )

    private fun loaded(envelopeId: String? = null) =
        EnvelopeTransfersViewModel(groupId = "g1", envelopeId = envelopeId, groups = groups, envelopes = envelopes, transfers = transfers)
            .also { it.onEvent(EnvelopeTransfersEvent.Refresh) }

    @Test
    fun fromAnEnvelopeOnlyItsMovesAreListedAndTheEnvelopeIsNamed() {
        val other = EnvelopeTransfer("t9", "u", "e9", 500, TransferOrigin.Manual, Instant.parse("2026-11-20T10:00:00Z"))
        transfers.transfers = listOf(other, transfer(1))
        val state = loaded(envelopeId = "e1").state.value

        assertEquals("getTransfers(g1, e1, 0, 50)", transfers.calls.last())
        assertEquals(listOf("t1"), state.rows.map { it.transfer.id })
        assertEquals("Food", state.envelopeName)
        assertFalse(state.envelopeIsUnassigned)
    }

    @Test
    fun theWholeGroupHasNoEnvelopeName() {
        transfers.transfers = listOf(transfer(1))
        val state = loaded().state.value

        assertEquals("getTransfers(g1, 0, 50)", transfers.calls.last())
        assertEquals(null, state.envelopeName)
    }

    @Test
    fun rowsResolveBothEnvelopesAndAnyMemberMayWrite() {
        transfers.transfers = listOf(transfer(2), transfer(1, TransferOrigin.Target))
        val state = loaded().state.value

        assertEquals(listOf("t2", "t1"), state.rows.map { it.transfer.id })
        assertEquals(unassigned, state.rows.first().from)
        assertEquals(food, state.rows.first().to)
        assertEquals(2, state.total)
        assertTrue(state.canWrite)
    }

    @Test
    fun anArchivedGroupIsReadOnly() {
        groups.groups = listOf(Group("g1", "Home", "EUR", GroupRole.Owner, archivedAt = Instant.parse("2026-11-01T00:00:00Z")))
        transfers.transfers = listOf(transfer(1))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeTransfersEvent.DeleteClicked("t1"))

        assertFalse(viewModel.state.value.canWrite)
        assertEquals(1, viewModel.state.value.rows.size)
    }

    @Test
    fun deletingATargetHidesItAndVoidsItWhenTheSnackbarEnds() {
        transfers.transfers = listOf(transfer(1, TransferOrigin.Target), transfer(2))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeTransfersEvent.DeleteClicked("t1"))

        assertEquals(listOf("t2"), viewModel.state.value.rows.map { it.transfer.id })
        assertEquals(Res.string.transfer_deleted, viewModel.state.value.undoMessage?.message)
        assertTrue(transfers.voided.isEmpty())

        viewModel.onEvent(EnvelopeTransfersEvent.UndoExpired)
        assertEquals(listOf("t1"), transfers.voided)
    }

    @Test
    fun undoBringsTheRowBackAndWritesNothing() {
        transfers.transfers = listOf(transfer(2), transfer(1))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeTransfersEvent.DeleteClicked("t1"))
        viewModel.onEvent(EnvelopeTransfersEvent.UndoClicked)

        assertEquals(listOf("t2", "t1"), viewModel.state.value.rows.map { it.transfer.id })
        assertTrue(transfers.voided.isEmpty())
    }

    @Test
    fun aCoverCannotBeDeleted() {
        transfers.transfers = listOf(transfer(1, TransferOrigin.Cover))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeTransfersEvent.DeleteClicked("t1"))

        assertEquals(1, viewModel.state.value.rows.size)
        assertFalse(viewModel.state.value.rows.single().canEdit)
    }

    @Test
    fun loadMoreAsksForTheNextPage() {
        transfers.transfers = (1..(EnvelopeTransfersViewModel.PAGE_SIZE + 5)).map { transfer(it % 9 + 1).copy(id = "t$it") }
        val viewModel = loaded()
        assertTrue(viewModel.state.value.hasMore)

        viewModel.onEvent(EnvelopeTransfersEvent.LoadMore)

        assertEquals(EnvelopeTransfersViewModel.PAGE_SIZE + 5, viewModel.state.value.rows.size)
        assertFalse(viewModel.state.value.hasMore)
    }
}
