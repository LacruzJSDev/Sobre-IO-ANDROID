package dev.lacruzjs.sobreio.ui.groupreview

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeCalendarRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeGroupSummaryRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.occurrence_recorded
import dev.lacruzjs.sobreio.generated.resources.occurrence_skipped
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import dev.lacruzjs.sobreio.ui.undo.PendingUndoCommits
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupReviewViewModelTest : MainDispatcherTest() {

    private val checking = Account("a1", "g1", "Checking", AccountKind.Bank, inEnvelopes = true, balanceCents = 500_000)
    private val cash = Account("a2", "g1", "Cash", AccountKind.Cash, inEnvelopes = false, balanceCents = 10_000)
    private val unassigned = Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 100_000)
    private val bills = Envelope("e1", "g1", "Bills", EnvelopeKind.Spending, 0, 3_000)
    private val food = Envelope("e2", "g1", "Food", EnvelopeKind.Spending, 0, 20_000)
    private val empty = Envelope("e3", "g1", "Empty", EnvelopeKind.Spending, 0, 0)
    private val old = Envelope("e4", "g1", "Old", EnvelopeKind.Spending, 0, 9_000, archivedAt = "2026-01-01T00:00:00Z")

    private val group = Group("g1", "Home", "EUR", GroupRole.Member)
    private val groupRepo = FakeGroupRepository(listOf(group))
    private val summary = FakeGroupSummaryRepository()
    private val calendar = FakeCalendarRepository()
    private val accounts = FakeAccountRepository(listOf(checking, cash))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, bills, food, empty, old))
    private val viewModel by lazy { GroupReviewViewModel("g1", groupRepo, summary, accounts, envelopes, calendar) }

    private fun due(
        id: String,
        kind: CommitmentKind = CommitmentKind.Expense,
        day: Int = 10,
        amount: Long = 5_000,
        account: String = "a1",
        envelope: String? = "e1",
    ) = DueOccurrence(id, "v-$id", "Name $id", kind, LocalDate(2026, 11, day), amount, account, null, envelope, false)

    private fun loaded(canRecord: Boolean = true): GroupReviewViewModel {
        // An archived group is read only
        if (!canRecord) groupRepo.groups = listOf(group.copy(archivedAt = Instant.parse("2026-11-01T00:00:00Z")))
        return viewModel.also { it.onEvent(GroupReviewEvent.Refresh) }
    }

    @Test
    fun pendingListKeepsTheDatabaseOrderAndDropsTheReceivingHalfOfAContribution() {
        summary.pending = listOf(due("c1", day = 5), due("c2", CommitmentKind.GroupTransferIn), due("c3", CommitmentKind.Income, day = 8))
        loaded()

        val state = viewModel.state.value
        assertEquals(listOf("c1", "c3"), state.pending.map { it.commitmentId })
                assertEquals(listOf(-5_000L, 5_000L), state.pending.map { it.signedCents })
        assertEquals("Checking", state.accountName("a1"))
    }

    @Test
    fun loadShowsTheGroupNameCurrencyAndPermission() {
        loaded()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Home", state.groupName)
        assertEquals("EUR", state.currencyCode)
        assertTrue(state.canRecord)
    }

    @Test
    fun failedLoadShowsTheErrorAndRetryReloads() {
        calendar.todayResult = Result.failure(AppError.Network)
        loaded()
        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)

        calendar.todayResult = null
        viewModel.onEvent(GroupReviewEvent.Refresh)

        assertNull(viewModel.state.value.error)
        assertEquals("Home", viewModel.state.value.groupName)
    }

    @Test
    fun aFailedRefreshKeepsTheRowsAndShowsTheErrorAbove() {
        summary.pending = listOf(due("c1"))
        loaded()
        calendar.todayResult = Result.failure(AppError.Network)

        viewModel.onEvent(GroupReviewEvent.Refresh)

        assertEquals(1, viewModel.state.value.pending.size)
        assertEquals(AppError.Network, viewModel.state.value.actionError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun reviewingEverythingLeavesAnEmptyList() {
        summary.pending = listOf(due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        assertTrue(viewModel.state.value.pending.isEmpty())
    }

    @Test
    fun backMakesAWaitingSkipFinal() = runTest {
        summary.pending = listOf(due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        viewModel.onEvent(GroupReviewEvent.BackClicked)
        // The skip is written outside the ViewModel's scope (it outlives the screen): wait for it like a reload does
        PendingUndoCommits.await()

        assertTrue(summary.calls.contains("skip(c1, 2026-11-10)"))
    }

    @Test
    fun confirmSheetStartsWithTheExpectedAmountAndTheDueDate() {
        summary.pending = listOf(due("c1", amount = 12_050))
        loaded()

        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))

        val sheet = viewModel.state.value.sheet
        assertEquals("120.50", sheet?.amount)
        assertEquals(LocalDate(2026, 11, 10), sheet?.date)
    }

    @Test
    fun aDueDateInTheFutureBeyondTodayStartsOnToday() {
        calendar.today = LocalDate(2026, 11, 14)
        summary.pending = listOf(due("c1", day = 20))
        loaded()

        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-20"))

        assertEquals(LocalDate(2026, 11, 14), viewModel.state.value.sheet?.date)
    }

    @Test
    fun confirmSendsTheEditedAmountAndDateAndRemovesTheRow() {
        summary.pending = listOf(due("c1"), due("c2", day = 11))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.AmountChanged("42,30"))
        viewModel.onEvent(GroupReviewEvent.DateChanged(LocalDate(2026, 11, 12)))

        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        assertEquals(4_230, summary.confirmedAmount)
        assertEquals(LocalDate(2026, 11, 12), summary.confirmedDate)
        assertEquals(emptyList(), summary.confirmedCover)
        val state = viewModel.state.value
        assertNull(state.sheet)
        assertEquals(listOf("c2"), state.pending.map { it.commitmentId })
        assertEquals(Res.string.occurrence_recorded, state.undoMessage?.message)
        assertEquals("Name c1", state.undoMessage?.argument)
    }

    @Test
    fun invalidAmountKeepsTheSheetOpenWithoutCalling() {
        summary.pending = listOf(due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.AmountChanged("abc"))

        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.sheet?.amountError)
        assertFalse(summary.calls.any { it.startsWith("confirm") })
    }

    @Test
    fun coverNoteAppearsOnlyWhenTheEnvelopeCannotCoverAnExpense() {
        summary.pending = listOf(due("c1", amount = 5_000, envelope = "e1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))

        val state = viewModel.state.value
        val draft = assertNotNull(state.sheet)
        assertEquals(bills, state.coverShortEnvelope(draft))
        assertEquals(listOf(food), state.coverCandidates(draft))

        viewModel.onEvent(GroupReviewEvent.AmountChanged("20"))
        assertNull(viewModel.state.value.let { it.coverShortEnvelope(it.sheet!!) })
    }

    @Test
    fun noCoverForUnassignedIncomeOrAccountsOutsideTheEnvelopes() {
        summary.pending = listOf(
            due("c1", envelope = "e0"),
            due("c2", CommitmentKind.Income, envelope = null),
            due("c3", account = "a2", envelope = "e1"),
        )
        loaded()
        val state = viewModel.state.value

        state.pending.forEach { assertNull(state.coverShortEnvelope(ConfirmDraft(it, "50", LocalDate(2026, 11, 10)))) }
    }

    @Test
    fun confirmCoversWithTheFullBalanceOfTheChosenEnvelope() {
        summary.pending = listOf(due("c1", amount = 5_000, envelope = "e1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.CoverSelected("e2"))

        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        assertEquals(listOf(CoverItem("e2", 20_000)), summary.confirmedCover)
    }

    @Test
    fun aChosenCoverIsIgnoredWhenTheAmountNoLongerNeedsIt() {
        summary.pending = listOf(due("c1", amount = 5_000, envelope = "e1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.CoverSelected("e2"))
        viewModel.onEvent(GroupReviewEvent.AmountChanged("10"))

        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        assertEquals(emptyList(), summary.confirmedCover)
    }

    @Test
    fun failedConfirmShowsTheErrorInTheSheetAndKeepsTheRow() {
        summary.pending = listOf(due("c1"))
        summary.confirmResult = Result.failure(AppError.BusinessRule("FZ019"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))

        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        val state = viewModel.state.value
        assertEquals(AppError.BusinessRule("FZ019"), state.sheet?.submitError)
        assertFalse(state.sheet?.isSubmitting ?: true)
        assertEquals(1, state.pending.size)
    }

    private fun undoOfConfirmationVoids(kind: CommitmentKind, expected: String) {
        summary.pending = listOf(due("c1", kind))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)

        viewModel.onEvent(GroupReviewEvent.UndoClicked)

        assertTrue(summary.calls.contains(expected), summary.calls.toString())
    }

    @Test
    fun undoingAnExpenseVoidsItsTransaction() = undoOfConfirmationVoids(CommitmentKind.Expense, "void(Expense, fact-c1)")

    @Test
    fun undoingAnIncomeVoidsItsTransaction() = undoOfConfirmationVoids(CommitmentKind.Income, "void(Income, fact-c1)")

    @Test
    fun undoingATransferVoidsItsAccountTransfer() = undoOfConfirmationVoids(CommitmentKind.Transfer, "void(Transfer, fact-c1)")

    @Test
    fun undoingAContributionVoidsItsGroupTransferLink() =
        undoOfConfirmationVoids(CommitmentKind.GroupTransferOut, "void(GroupTransferOut, fact-c1)")

    @Test
    fun undoOfAConfirmationReadsThePendingListAgain() {
        summary.pending = listOf(due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.ConfirmSubmitted)
        assertTrue(viewModel.state.value.pending.isEmpty())

        // The database frees the due date when its fact is voided
        summary.pending = listOf(due("c1"))
        viewModel.onEvent(GroupReviewEvent.UndoClicked)

        assertEquals(listOf("c1"), viewModel.state.value.pending.map { it.commitmentId })
    }

    @Test
    fun skipHidesTheRowAtOnceButWritesNothingYet() {
        summary.pending = listOf(due("c1"), due("c2", day = 11))
        loaded()

        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        val state = viewModel.state.value
        assertEquals(listOf("c2"), state.pending.map { it.commitmentId })
        assertEquals(Res.string.occurrence_skipped, state.undoMessage?.message)
        assertFalse(summary.calls.any { it.startsWith("skip") })
    }

    @Test
    fun skipIsWrittenWhenTheSnackbarEndsWithoutUndo() {
        summary.pending = listOf(due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        viewModel.onEvent(GroupReviewEvent.UndoExpired)

        assertTrue(summary.calls.contains("skip(c1, 2026-11-10)"))
        assertTrue(viewModel.state.value.pending.isEmpty())
    }

    @Test
    fun undoOfASkipBringsTheRowBackWithoutWriting() {
        summary.pending = listOf(due("c2", day = 11), due("c1"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        viewModel.onEvent(GroupReviewEvent.UndoClicked)

        assertEquals(listOf("c1", "c2"), viewModel.state.value.pending.map { it.commitmentId })
        assertFalse(summary.calls.any { it.startsWith("skip") })
    }

    @Test
    fun aReloadWhileASkipWaitsDoesNotBringTheRowBack() {
        summary.pending = listOf(due("c1"), due("c2", day = 11))
        loaded()
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        // Reads again without going through the commit: the skip is still only on the screen
        viewModel.onEvent(GroupReviewEvent.Refresh)

        assertFalse(viewModel.state.value.pending.any { it.commitmentId == "c1" })
    }

    @Test
    fun failedSkipRestoresTheRowAndShowsTheError() {
        summary.pending = listOf(due("c1"))
        summary.skipResult = Result.failure(AppError.BusinessRule("FZ012"))
        loaded()
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        viewModel.onEvent(GroupReviewEvent.UndoExpired)

        assertEquals(listOf("c1"), viewModel.state.value.pending.map { it.commitmentId })
        assertEquals(AppError.BusinessRule("FZ012"), viewModel.state.value.actionError)
    }

    @Test
    fun readOnlyGroupCannotConfirmOrSkip() {
        summary.pending = listOf(due("c1"))
        loaded(canRecord = false)

        viewModel.onEvent(GroupReviewEvent.ConfirmClicked("c1|2026-11-10"))
        viewModel.onEvent(GroupReviewEvent.SkipClicked("c1|2026-11-10"))

        assertNull(viewModel.state.value.sheet)
        assertEquals(1, viewModel.state.value.pending.size)
    }
}
