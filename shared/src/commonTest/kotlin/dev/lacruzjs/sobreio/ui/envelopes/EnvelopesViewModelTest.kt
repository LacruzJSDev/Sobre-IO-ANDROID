package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelope_deleted
import dev.lacruzjs.sobreio.generated.resources.envelope_reopened
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_negative
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.generated.resources.validation_source_invalid
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvelopesViewModelTest : MainDispatcherTest() {

    private val unassigned = Envelope("0", "g1", "Sin asignar", EnvelopeKind.System, 0, 30_000)
    private val rent = Envelope("1", "g1", "Rent", EnvelopeKind.Spending, 50_000, 10_000)
    private val food = Envelope("2", "g1", "food", EnvelopeKind.Spending, 20_000, 5_000)
    private val trip = Envelope("3", "g1", "Trip", EnvelopeKind.Saving, 0, 80_000)
    private val repository = FakeEnvelopeRepository(listOf(unassigned, trip, rent, food))
        .also { it.summary = EnvelopeSummary(totalCents = 125_000, unassignedCents = 30_000) }
    private val memberRepository = FakeMemberRepository()
    private val old = Envelope("4", "g1", "Old", EnvelopeKind.Saving, 0, 0, archivedAt = "2026-11-01T10:00:00Z")
    private val viewModel by lazy { EnvelopesViewModel("g1", repository, memberRepository) }

    private fun loaded(canEdit: Boolean = true): EnvelopesViewModel = viewModel.also {
        it.onEvent(EnvelopesEvent.GroupLoaded("USD", canEdit))
        it.onEvent(EnvelopesEvent.Refresh)
    }

    @Test
    fun refreshMaterializesTargetsBeforeReading() {
        loaded()

        assertEquals(listOf("materialize(g1)", "getEnvelopes(g1)", "getSummary(g1)"), repository.calls)
    }

    @Test
    fun failedMaterializeIsIgnored() {
        repository.materializeResult = Result.failure(AppError.Network)
        loaded()

        assertNull(viewModel.state.value.error)
        assertEquals(3, viewModel.state.value.envelopes.size)
    }

    @Test
    fun loadSplitsUnassignedFromTheSectionsAndOrdersByName() {
        loaded()

        val state = viewModel.state.value
        assertEquals(unassigned, state.unassigned)
        assertEquals(listOf(food, rent), state.spending)
        assertEquals(listOf(trip), state.saving)
        assertEquals(125_000, state.totalCents)
        assertEquals(30_000, state.unassignedCents)
        assertEquals("USD", state.currencyCode)
        assertFalse(state.isLoading)
    }

    @Test
    fun failedLoadShowsTheError() {
        repository.envelopesResult = Result.failure(AppError.Network)
        loaded()

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun deleteArchivesAtOnceHidesTheRowAndOffersUndo() {
        loaded()
        viewModel.onEvent(EnvelopesEvent.DeleteClicked("1"))

        assertTrue("archiveEnvelope(1)" in repository.calls)
        assertEquals(listOf(food), viewModel.state.value.spending)
        assertEquals(Res.string.envelope_deleted, viewModel.state.value.undoMessage?.message)
        assertEquals("Rent", viewModel.state.value.undoMessage?.argument)
    }

    @Test
    fun undoUnarchivesAndBringsTheRowBack() {
        loaded()
        viewModel.onEvent(EnvelopesEvent.DeleteClicked("1"))
        viewModel.onEvent(EnvelopesEvent.UndoClicked)

        assertTrue("unarchiveEnvelope(1)" in repository.calls)
        assertEquals(listOf(food, rent), viewModel.state.value.spending)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun deleteRefusedByTheDatabaseRestoresTheRowWithTheError() {
        repository.archiveResult = Result.failure(AppError.BusinessRule("FZ008"))
        loaded()
        viewModel.onEvent(EnvelopesEvent.DeleteClicked("1"))

        assertEquals(listOf(food, rent), viewModel.state.value.spending)
        assertEquals(AppError.BusinessRule("FZ008"), viewModel.state.value.actionError)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun assignSendsTheAmountFromUnassignedByDefaultThenReloads() {
        loaded(canEdit = false)
        viewModel.onEvent(EnvelopesEvent.AssignClicked("1"))

        assertEquals(EnvelopesSheet.Assign, viewModel.state.value.sheet)
        assertEquals("0", viewModel.state.value.sourceId)
        assertEquals(listOf("0", "2", "3"), viewModel.state.value.assignSources.map { it.id })

        viewModel.onEvent(EnvelopesEvent.AmountChanged("25.5"))
        repository.calls.clear()
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)

        assertEquals(
            listOf("assign(g1, 0, 1, 2550)", "materialize(g1)", "getEnvelopes(g1)", "getSummary(g1)"),
            repository.calls,
        )
        assertEquals(EnvelopesSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun assignCanTakeFromAnotherEnvelope() {
        loaded()
        viewModel.onEvent(EnvelopesEvent.AssignClicked("1"))
        viewModel.onEvent(EnvelopesEvent.SourceSelected("3"))
        viewModel.onEvent(EnvelopesEvent.AmountChanged("10"))
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)

        assertTrue("assign(g1, 3, 1, 1000)" in repository.calls)
    }

    @Test
    fun assignRejectsZeroNegativeAndInvalidAmounts() {
        loaded()
        viewModel.onEvent(EnvelopesEvent.AssignClicked("1"))
        repository.calls.clear()

        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)
        assertEquals(Res.string.validation_amount_positive, viewModel.state.value.amountError)

        viewModel.onEvent(EnvelopesEvent.AmountChanged("-3"))
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)
        assertEquals(Res.string.validation_amount_positive, viewModel.state.value.amountError)

        viewModel.onEvent(EnvelopesEvent.AmountChanged("1.298,15"))
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)
        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.amountError)

        assertTrue(repository.calls.isEmpty())
        assertEquals(EnvelopesSheet.Assign, viewModel.state.value.sheet)
    }

    @Test
    fun assignRejectsSourceEqualToDestination() {
        loaded()
        viewModel.onEvent(EnvelopesEvent.AssignClicked("1"))
        viewModel.onEvent(EnvelopesEvent.SourceSelected("1"))
        viewModel.onEvent(EnvelopesEvent.AmountChanged("5"))
        repository.calls.clear()
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)

        assertEquals(Res.string.validation_source_invalid, viewModel.state.value.sourceError)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun failedAssignKeepsTheSheetOpenWithTheError() {
        repository.assignResult = Result.failure(AppError.BusinessRule("FZ002"))
        loaded()
        viewModel.onEvent(EnvelopesEvent.AssignClicked("1"))
        viewModel.onEvent(EnvelopesEvent.AmountChanged("5"))
        viewModel.onEvent(EnvelopesEvent.AssignConfirmed)

        assertEquals(EnvelopesSheet.Assign, viewModel.state.value.sheet)
        assertEquals(AppError.BusinessRule("FZ002"), viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun membersCannotDelete() {
        loaded(canEdit = false)
        viewModel.onEvent(EnvelopesEvent.DeleteClicked("1"))

        assertEquals(listOf(food, rent), viewModel.state.value.spending)
        assertTrue(repository.calls.none { it.startsWith("archive") || it.startsWith("create") })
    }

    @Test
    fun unassignedIsKeptApartSoTheListShowsItFirst() {
        loaded()

        val state = viewModel.state.value
        assertEquals("0", state.unassigned?.id)
        assertTrue(state.envelopes.none { it.kind == EnvelopeKind.System })
        assertEquals(30_000, state.unassignedCents)
    }

    @Test
    fun loadKeepsTheColourIconAndHolderOfEachEnvelope() {
        val styled = food.copy(color = "#4F7A5A", icon = "groceries", ownerUserId = "u2")
        repository.envelopes = listOf(unassigned, styled)
        loaded()

        assertEquals(styled, viewModel.state.value.envelopes.single())
    }

    @Test
    fun archivedEnvelopesGoToTheArchivedSectionNotTheSections() {
        repository.envelopes = listOf(unassigned, rent, old)
        loaded()

        assertEquals(listOf(rent), viewModel.state.value.envelopes)
        assertEquals(listOf(old), viewModel.state.value.archivedEnvelopes)
    }

    @Test
    fun reopenUnarchivesMovesTheRowToItsSectionAndOffersUndo() {
        repository.envelopes = listOf(unassigned, rent, old)
        loaded()
        viewModel.onEvent(EnvelopesEvent.ReopenClicked("4"))

        assertTrue("unarchiveEnvelope(4)" in repository.calls)
        assertEquals(listOf("Old"), viewModel.state.value.saving.map { it.name })
        assertTrue(viewModel.state.value.archivedEnvelopes.isEmpty())
        assertEquals(Res.string.envelope_reopened, viewModel.state.value.undoMessage?.message)
        assertEquals("Old", viewModel.state.value.undoMessage?.argument)
    }

    @Test
    fun undoOfReopenArchivesTheEnvelopeAgain() {
        repository.envelopes = listOf(unassigned, rent, old)
        loaded()
        viewModel.onEvent(EnvelopesEvent.ReopenClicked("4"))
        repository.envelopes = listOf(unassigned, rent, old)
        viewModel.onEvent(EnvelopesEvent.UndoClicked)

        assertTrue("archiveEnvelope(4)" in repository.calls)
        assertEquals(listOf(old), viewModel.state.value.archivedEnvelopes)
    }

    @Test
    fun reopenRefusedByTheDatabaseKeepsTheRowArchivedWithTheError() {
        repository.envelopes = listOf(unassigned, old)
        repository.unarchiveResult = Result.failure(AppError.DuplicateName)
        loaded()
        viewModel.onEvent(EnvelopesEvent.ReopenClicked("4"))

        assertEquals(listOf(old), viewModel.state.value.archivedEnvelopes)
        assertEquals(AppError.DuplicateName, viewModel.state.value.actionError)
    }

    @Test
    fun membersCannotReopen() {
        repository.envelopes = listOf(unassigned, old)
        loaded(canEdit = false)
        viewModel.onEvent(EnvelopesEvent.ReopenClicked("4"))

        assertTrue(repository.calls.none { it.startsWith("unarchive") })
    }

    @Test
    fun holderNamesAreOnlyShownWhenTheGroupHasSeveralMembers() {
        val held = rent.copy(ownerUserId = "u2")
        repository.envelopes = listOf(unassigned, held)
        memberRepository.members = listOf(Member("u1", "Juan", GroupRole.Owner))
        loaded()
        assertNull(viewModel.state.value.holderName(held))

        memberRepository.members = listOf(Member("u1", "Juan", GroupRole.Owner), Member("u2", "Marta", GroupRole.Member))
        viewModel.onEvent(EnvelopesEvent.Refresh)
        assertEquals("Marta", viewModel.state.value.holderName(held))
        assertNull(viewModel.state.value.holderName(rent))
    }
}
