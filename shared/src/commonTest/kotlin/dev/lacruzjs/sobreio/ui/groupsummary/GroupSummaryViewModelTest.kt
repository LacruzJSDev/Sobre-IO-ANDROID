package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeProgress
import dev.lacruzjs.sobreio.domain.model.EnvelopeSpending
import dev.lacruzjs.sobreio.domain.model.FreePoint
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.ProgressState
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.BoundaryCrossing
import dev.lacruzjs.sobreio.domain.model.TransferHint
import dev.lacruzjs.sobreio.domain.model.TransferSuggestion
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupSummaryRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupSummaryViewModelTest : MainDispatcherTest() {

    private val checking = Account("a1", "g1", "Checking", AccountKind.Bank, inEnvelopes = true, balanceCents = 500_000)
    private val unassigned = Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 100_000)
    private val bills = Envelope("e1", "g1", "Bills", EnvelopeKind.Spending, 0, 3_000)
    private val food = Envelope("e2", "g1", "Food", EnvelopeKind.Spending, 0, 20_000)

    private val summary = FakeGroupSummaryRepository()
    private val detail = FakeAccountDetailRepository()
    private val accounts = FakeAccountRepository(listOf(checking))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, bills, food))
    private val viewModel by lazy { GroupSummaryViewModel("g1", summary, detail, accounts, envelopes) }

    private fun due(id: String, kind: CommitmentKind = CommitmentKind.Expense, day: Int = 10, amount: Long = 5_000) =
        DueOccurrence(id, "v-$id", "Name $id", kind, LocalDate(2026, 11, day), amount, "a1", null, "e1", false)

    private fun loaded(canRecord: Boolean = true): GroupSummaryViewModel = viewModel.also {
        it.onEvent(GroupSummaryEvent.GroupLoaded("EUR", canRecord))
        it.onEvent(GroupSummaryEvent.Refresh)
    }

    private fun point(day: Int, free: Long) = FreePoint(LocalDate(2026, 11, day), free)

    @Test
    fun loadShowsTheOutlookFigures() {
        loaded()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(100_000, state.outlook?.freeTodayCents)
        assertEquals(80_000, state.outlook?.freeProjectedCents)
        assertEquals(LocalDate(2026, 11, 30), state.outlook?.periodEnd)
        assertEquals("EUR", state.currencyCode)
    }

    @Test
    fun theHeroKnowsTheDayOfThePeriodAndItsProgress() {
        loaded()

        val state = viewModel.state.value
        assertEquals(14, state.periodDay)
        assertEquals(30, state.periodLength)
        assertEquals(14f / 30f, state.periodProgress)
    }

    @Test
    fun periodProgressIsFullOnTheLastDay() {
        detail.today = LocalDate(2026, 11, 30)
        loaded()

        assertEquals(30, viewModel.state.value.periodDay)
        assertEquals(1f, viewModel.state.value.periodProgress)
    }

    @Test
    fun theProjectionRowsArriveSortedByDay() {
        summary.freeProjection = listOf(point(16, 90_000), point(14, 100_000), point(15, 100_000))
        loaded()

        assertEquals(listOf(14, 15, 16), viewModel.state.value.freeProjection.map { it.day.day })
        assertEquals(listOf("getFreeProjection(g1)"), summary.calls.filter { it.startsWith("getFreeProjection") })
    }

    @Test
    fun theLowestPointIsTheSmallestRowWhenItGoesBelowZero() {
        summary.freeProjection = listOf(point(14, 100_000), point(18, -5_000), point(22, -9_000), point(30, -2_000))
        loaded()

        assertEquals(point(22, -9_000), viewModel.state.value.lowestPoint)
    }

    @Test
    fun noLowestPointWhenTheProjectionStaysAboveZero() {
        summary.freeProjection = listOf(point(14, 100_000), point(20, 40_000), point(30, 80_000))
        loaded()

        assertNull(viewModel.state.value.lowestPoint)
    }

    @Test
    fun noLowestPointWhenTodayIsAlreadyTheLowest() {
        summary.freeProjection = listOf(point(14, -9_000), point(20, -5_000), point(30, -5_000))
        loaded()

        assertNull(viewModel.state.value.lowestPoint)
    }

    @Test
    fun theNoteIsTheCommitmentsShortfallFirst() {
        summary.outlook = summary.outlook.copy(committedShortfallCents = 7_000, pendingOutCents = 3_000, pendingIncomeCents = 9_000)
        loaded()

        assertEquals(ProjectionNote(ProjectionNoteKind.Commitments, 7_000), viewModel.state.value.projectionNote)
    }

    @Test
    fun theNoteFallsBackToContributionsThenIncomeThenNothing() {
        summary.outlook = summary.outlook.copy(pendingOutCents = 3_000, pendingIncomeCents = 9_000)
        loaded()
        assertEquals(ProjectionNote(ProjectionNoteKind.Contributions, 3_000), viewModel.state.value.projectionNote)

        summary.outlook = summary.outlook.copy(pendingOutCents = 0)
        viewModel.onEvent(GroupSummaryEvent.Refresh)
        assertEquals(ProjectionNote(ProjectionNoteKind.Income, 9_000), viewModel.state.value.projectionNote)

        summary.outlook = summary.outlook.copy(pendingIncomeCents = 0)
        viewModel.onEvent(GroupSummaryEvent.Refresh)
        assertNull(viewModel.state.value.projectionNote)
    }

    @Test
    fun reviewCardIsCalmWithNothingPending() {
        loaded()

        val state = viewModel.state.value
        assertEquals(0, state.pendingCount)
        assertNull(state.oldestPendingDate)
    }

    @Test
    fun reviewCardCountsThePendingAndKnowsTheOldestDate() {
        summary.pending = listOf(due("c1", day = 5), due("c2", CommitmentKind.GroupTransferIn), due("c3", CommitmentKind.Income, day = 8))
        loaded()

        val state = viewModel.state.value
        assertEquals(2, state.pendingCount)
        assertEquals(LocalDate(2026, 11, 5), state.oldestPendingDate)
        assertEquals(listOf(-5_000L, 5_000L), state.pending.map { it.signedCents })
    }

    @Test
    fun reviewAndNavigationEventsChangeNothingInTheViewModel() {
        summary.pending = listOf(due("c1"))
        loaded()
        val before = viewModel.state.value

        viewModel.onEvent(GroupSummaryEvent.ReviewClicked)
        viewModel.onEvent(GroupSummaryEvent.SpendingClicked("e2"))
        viewModel.onEvent(GroupSummaryEvent.SeeAllEnvelopesClicked)

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun spendingIsAskedForThePeriodStartAndSortedByAmount() {
        summary.spending = listOf(EnvelopeSpending("e1", 3_000), EnvelopeSpending("e2", 12_000), EnvelopeSpending("e0", 500))
        loaded()

        assertEquals(LocalDate(2026, 11, 1), summary.spendingPeriodStart)
        assertEquals(listOf("Food", "Bills", "Sin asignar"), viewModel.state.value.spending.map { it.envelope.name })
        assertEquals(listOf(12_000L, 3_000L, 500L), viewModel.state.value.spending.map { it.spentCents })
    }

    @Test
    fun spendingKeepsOnlyTheTopSixAndBreaksTiesByName() {
        val many = (1..8).map { Envelope("x$it", "g1", "Env $it", EnvelopeKind.Spending, 0, 0) }
        envelopes.envelopes = many
        summary.spending = many.mapIndexed { i, e -> EnvelopeSpending(e.id, if (i < 2) 1_000L else 1_000L * (i + 1)) }
        loaded()

        val names = viewModel.state.value.spending.map { it.envelope.name }
        assertEquals(6, names.size)
        // 8_000, 7_000, 6_000, 5_000, 4_000, 3_000 and the two 1_000 rows are cut
        assertEquals(listOf("Env 8", "Env 7", "Env 6", "Env 5", "Env 4", "Env 3"), names)
    }

    @Test
    fun equalSpendingIsOrderedByName() {
        summary.spending = listOf(EnvelopeSpending("e2", 1_000), EnvelopeSpending("e1", 1_000))
        loaded()

        assertEquals(listOf("Bills", "Food"), viewModel.state.value.spending.map { it.envelope.name })
    }

    @Test
    fun spendingBarsAreProportionalToTheBiggest() {
        summary.spending = listOf(EnvelopeSpending("e1", 3_000), EnvelopeSpending("e2", 12_000))
        loaded()

        val bars = viewModel.state.value.spending
        assertEquals(1f, viewModel.state.value.spendingFraction(bars[0]))
        assertEquals(0.25f, viewModel.state.value.spendingFraction(bars[1]))
    }

    @Test
    fun spendingOfAnEnvelopeThatIsNotListedIsLeftOut() {
        summary.spending = listOf(EnvelopeSpending("gone", 9_000), EnvelopeSpending("e1", 3_000))
        loaded()

        assertEquals(listOf("e1"), viewModel.state.value.spending.map { it.envelope.id })
    }

    @Test
    fun nothingSpentLeavesNoBars() {
        loaded()

        assertTrue(viewModel.state.value.spending.isEmpty())
    }

    @Test
    fun aFailedSpendingReadLeavesOnlyThatCardOut() {
        summary.spendingResult = Result.failure(AppError.Network)
        loaded()

        val state = viewModel.state.value
        assertNull(state.error)
        assertTrue(state.spending.isEmpty())
        assertNotNull(state.outlook)
    }

    @Test
    fun flowsAreTheLastSixPeriodsOfTheDatabase() {
        summary.periodFlows = listOf(
            PeriodFlow("p1", LocalDate(2026, 10, 1), LocalDate(2026, 10, 31), 200_000, 150_000),
            PeriodFlow("p2", LocalDate(2026, 11, 1), LocalDate(2026, 11, 30), 100_000, 90_000),
        )
        loaded()

        assertEquals(6, summary.flowPeriods)
        assertEquals(listOf("p1", "p2"), viewModel.state.value.flows.map { it.periodId })
    }

    @Test
    fun aFailedFlowsReadLeavesOnlyThatCardOut() {
        summary.flowsResult = Result.failure(AppError.Network)
        loaded()

        assertTrue(viewModel.state.value.flows.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun upcomingGoesFromTomorrowToThePeriodEnd() {
        summary.upcoming = listOf(due("c9", day = 20))
        loaded()

        assertEquals(LocalDate(2026, 11, 15) to LocalDate(2026, 11, 30), summary.upcomingRange)
        assertEquals(listOf("c9"), viewModel.state.value.upcoming.map { it.commitmentId })
    }

    @Test
    fun noUpcomingIsAskedWhenTodayIsTheLastDay() {
        detail.today = LocalDate(2026, 11, 30)
        loaded()

        assertNull(summary.upcomingRange)
        assertTrue(viewModel.state.value.upcoming.isEmpty())
    }

    @Test
    fun warningsComeFromEnvelopesAccountsAndCommitments() {
        envelopes.progress = listOf(EnvelopeProgress("e2", null, ProgressState.Overspent, -2_500, 0))
        summary.shortfalls = listOf(AccountShortfall("a1", 5_000, -5_000))
        detail.projection = listOf(
            ProjectionPoint(LocalDate(2026, 11, 14), 1_000, 0),
            ProjectionPoint(LocalDate(2026, 11, 22), -5_000, -6_000),
        )
        summary.outlook = summary.outlook.copy(committedShortfallCents = 7_000)
        loaded()

        assertEquals(
            listOf(
                GroupWarning.EnvelopeOverspent("e2", "Food", 2_500),
                GroupWarning.AccountShortfallOn("a1", "Checking", -5_000, LocalDate(2026, 11, 22)),
                GroupWarning.CommitmentsShortfall(7_000),
            ),
            viewModel.state.value.warnings,
        )
    }

    private val savings = Account("a2", "g1", "Savings", AccountKind.Savings, inEnvelopes = false, balanceCents = 900_000)
    private val cash = Account("a3", "g1", "Cash", AccountKind.Cash, inEnvelopes = true, balanceCents = 100_000)

    private fun shortfallOnChecking() {
        accounts.accounts = listOf(checking, savings, cash)
        summary.shortfalls = listOf(AccountShortfall("a1", 5_000, -5_000))
        detail.projection = listOf(
            ProjectionPoint(LocalDate(2026, 11, 14), 1_000, 0),
            ProjectionPoint(LocalDate(2026, 11, 22), -5_000, -6_000),
        )
    }

    @Test
    fun anAccountWarningCarriesTheSuggestionsOfThePlanForThatAccountOnly() {
        shortfallOnChecking()
        val fromCash = TransferSuggestion("a3", "a1", 3_000, LocalDate(2026, 11, 22), crossesBoundary = false)
        val fromSavings = TransferSuggestion("a2", "a1", 2_000, LocalDate(2026, 11, 22), crossesBoundary = true)
        val other = TransferSuggestion("a2", "a3", 9_000, LocalDate(2026, 11, 20), crossesBoundary = true)
        summary.transferPlan = listOf(fromCash, fromSavings, other)
        loaded()

        val warning = viewModel.state.value.warnings.filterIsInstance<GroupWarning.AccountShortfallOn>().single()
        assertEquals(
            listOf(
                TransferHint(fromCash, "Cash", null),
                // The receiver is inside the envelopes and the donor outside: the money enters the distribution
                TransferHint(fromSavings, "Savings", BoundaryCrossing.Enters),
            ),
            warning.hints,
        )
    }

    @Test
    fun aTransferOutOfTheEnvelopesLeavesTheDistribution() {
        accounts.accounts = listOf(checking.copy(inEnvelopes = false), cash)
        summary.shortfalls = listOf(AccountShortfall("a1", 5_000, -5_000))
        val suggestion = TransferSuggestion("a3", "a1", 5_000, LocalDate(2026, 11, 22), crossesBoundary = true)
        summary.transferPlan = listOf(suggestion)
        loaded()

        val warning = viewModel.state.value.warnings.filterIsInstance<GroupWarning.AccountShortfallOn>().single()
        assertEquals(listOf(TransferHint(suggestion, "Cash", BoundaryCrossing.Leaves)), warning.hints)
    }

    @Test
    fun theUncoveredPartHasNoSourceAccount() {
        shortfallOnChecking()
        val missing = TransferSuggestion(null, "a1", 1_500, LocalDate(2026, 11, 22), crossesBoundary = false)
        summary.transferPlan = listOf(missing)
        loaded()

        val warning = viewModel.state.value.warnings.filterIsInstance<GroupWarning.AccountShortfallOn>().single()
        assertEquals(listOf(TransferHint(missing, null, null)), warning.hints)
    }

    @Test
    fun aFailedPlanLeavesTheWarningWithoutSuggestions() {
        shortfallOnChecking()
        summary.transferPlanResult = Result.failure(AppError.Network)
        loaded()

        val warning = viewModel.state.value.warnings.filterIsInstance<GroupWarning.AccountShortfallOn>().single()
        assertTrue(warning.hints.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun theTransferButtonIsHandledByTheRouteAndWritesNothing() {
        shortfallOnChecking()
        val suggestion = TransferSuggestion("a3", "a1", 3_000, LocalDate(2026, 11, 22), crossesBoundary = false)
        summary.transferPlan = listOf(suggestion)
        loaded()
        val before = viewModel.state.value

        viewModel.onEvent(GroupSummaryEvent.TransferClicked(suggestion))

        assertEquals(before, viewModel.state.value)
        assertTrue(summary.calls.none { it.startsWith("confirm") || it.startsWith("skip") || it.startsWith("void") })
    }

    @Test
    fun noWarningsWhenEverythingIsFine() {
        loaded()

        assertTrue(viewModel.state.value.warnings.isEmpty())
    }

    @Test
    fun failedLoadShowsTheErrorAndRetryReloads() {
        summary.outlookResult = Result.failure(AppError.Network)
        loaded()
        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)

        summary.outlookResult = null
        viewModel.onEvent(GroupSummaryEvent.Refresh)

        assertNull(viewModel.state.value.error)
        assertNotNull(viewModel.state.value.outlook)
    }

    @Test
    fun aFailedRefreshKeepsTheContentAndShowsTheError() {
        loaded()
        summary.outlookResult = Result.failure(AppError.Network)

        viewModel.onEvent(GroupSummaryEvent.Refresh)

        val state = viewModel.state.value
        assertNotNull(state.outlook)
        assertNull(state.error)
        assertEquals(AppError.Network, state.actionError)
    }
}
