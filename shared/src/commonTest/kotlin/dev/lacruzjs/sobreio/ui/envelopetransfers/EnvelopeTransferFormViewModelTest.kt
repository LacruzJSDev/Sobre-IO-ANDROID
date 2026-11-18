package dev.lacruzjs.sobreio.ui.envelopetransfers

import dev.lacruzjs.sobreio.domain.error.AppError
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
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_envelopes_differ
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class EnvelopeTransferFormViewModelTest : MainDispatcherTest() {

    private val unassigned = Envelope("u", "g1", "Sin asignar", EnvelopeKind.System, 0, 0)
    private val food = Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 40_000, 0)
    private val trip = Envelope("e2", "g1", "Trip", EnvelopeKind.Saving, 0, 5_000)
    private val old = Envelope("e3", "g1", "Old", EnvelopeKind.Spending, 0, 0, archivedAt = "2026-10-01T00:00:00Z")
    private val groups = FakeGroupRepository(listOf(Group("g1", "Home", "EUR", GroupRole.Member)))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food, trip, old))
    private val target = EnvelopeTransfer("t1", "u", "e1", 40_000, TransferOrigin.Target, Instant.parse("2026-11-01T08:00:00Z"))
    private val manual = EnvelopeTransfer("t2", "e1", "e2", 1_250, TransferOrigin.Manual, Instant.parse("2026-11-02T08:00:00Z"), notes = "Gift")
    private val transfers = FakeEnvelopeTransferRepository(listOf(target, manual))

    private fun form(transferId: String? = null, initialToId: String? = null) = EnvelopeTransferFormViewModel(
        groupId = "g1",
        transferId = transferId,
        initialToId = initialToId,
        groups = groups,
        envelopes = envelopes,
        transfers = transfers,
    )

    @Test
    fun aNewMoveFromAnEnvelopesListStartsWithThatEnvelopeAsTheDestination() {
        val state = form(initialToId = "e2").state.value

        assertEquals("u", state.fromId)
        assertEquals("e2", state.toId)
    }

    @Test
    fun anArchivedOrUnassignedDestinationIsNotPreselected() {
        assertEquals(null, form(initialToId = "e3").state.value.toId)
        assertEquals(null, form(initialToId = "u").state.value.toId)
    }

    @Test
    fun aNewMoveStartsFromUnassignedAndOffersOnlyLiveEnvelopes() {
        val state = form().state.value

        assertEquals("u", state.fromId)
        assertEquals(listOf("u", "e1", "e2"), state.envelopes.map { it.id })
        assertFalse(state.envelopesLocked)
    }

    @Test
    fun createSendsAManualMove() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeTransferFormEvent.ToSelected("e2"))
        viewModel.onEvent(EnvelopeTransferFormEvent.AmountChanged("12,5"))
        viewModel.onEvent(EnvelopeTransferFormEvent.NotesChanged("  "))
        viewModel.onEvent(EnvelopeTransferFormEvent.SaveClicked)

        assertEquals("createTransfer(g1, u, e2, 1250)", transfers.calls.last())
        assertEquals(null, transfers.createdDraft?.notes)
        assertTrue(viewModel.state.value.saved)
    }

    @Test
    fun validationNeedsAnAmountADestinationAndTwoDifferentEnvelopes() {
        val viewModel = form()
        viewModel.onEvent(EnvelopeTransferFormEvent.SaveClicked)
        assertEquals(Res.string.validation_required, viewModel.state.value.toError)

        viewModel.onEvent(EnvelopeTransferFormEvent.ToSelected("u"))
        viewModel.onEvent(EnvelopeTransferFormEvent.AmountChanged("0"))
        viewModel.onEvent(EnvelopeTransferFormEvent.SaveClicked)

        assertEquals(Res.string.validation_envelopes_differ, viewModel.state.value.toError)
        assertEquals(Res.string.validation_amount_positive, viewModel.state.value.amountError)
        assertTrue(transfers.calls.none { it.startsWith("createTransfer") })
    }

    @Test
    fun editingATargetKeepsItsEnvelopesAndChangesTheAmount() {
        val viewModel = form("t1")
        assertTrue(viewModel.state.value.envelopesLocked)
        assertEquals("400", viewModel.state.value.amount)

        viewModel.onEvent(EnvelopeTransferFormEvent.ToSelected("e2"))
        viewModel.onEvent(EnvelopeTransferFormEvent.AmountChanged("150"))
        viewModel.onEvent(EnvelopeTransferFormEvent.SaveClicked)

        assertEquals("updateTransfer(t1, u, e1, 15000)", transfers.calls.last())
    }

    @Test
    fun editingAManualMoveLoadsItsFieldsAndKeepsAStoredArchivedEnvelope() {
        transfers.transfers = listOf(manual.copy(toEnvelopeId = "e3"))
        val state = form("t2").state.value

        assertEquals("e1", state.fromId)
        assertEquals("e3", state.toId)
        assertEquals("12.50", state.amount)
        assertEquals("Gift", state.notes)
        assertTrue("e3" in state.envelopes.map { it.id })
    }

    @Test
    fun aCoverCannotBeOpenedInTheForm() {
        transfers.transfers = listOf(target.copy(origin = TransferOrigin.Cover))
        assertEquals(AppError.NotFound, form("t1").state.value.loadError)
    }
}
