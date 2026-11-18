package dev.lacruzjs.sobreio.ui.envelopedetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.CommitmentFunding
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage
import dev.lacruzjs.sobreio.domain.model.FundingCommitment
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeCommitmentRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeDetailRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.fake.FakeMovementRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.transfer_deleted
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EnvelopeDetailViewModelTest : MainDispatcherTest() {

    private val unassigned = Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 25_000)
    private val food = Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 40_000, 12_050, color = "#4F7A5A", icon = "groceries", ownerUserId = "u1")
    private val trip = Envelope("e2", "g1", "Trip", EnvelopeKind.Saving, 0, 80_000)
    private val old = Envelope("e3", "g1", "Old", EnvelopeKind.Spending, 0, 0, archivedAt = "2026-10-01T10:00:00Z")
    private val ana = Member("u1", "Ana", GroupRole.Owner)
    private val leo = Member("u2", "Leo", GroupRole.Member)
    private val bank = Account("a1", "g1", "Bank", AccountKind.Bank, true, 0)
    private val cash = Account("a2", "g1", "Cash", AccountKind.Cash, true, 0)

    private val groups = FakeGroupRepository(listOf(Group("g1", "Home", "EUR", GroupRole.Owner)))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food, trip, old))
    private val members = FakeMemberRepository(listOf(ana, leo))
    private val accounts = FakeAccountRepository(listOf(bank, cash))
    private val ledger = FakeMovementRepository()
    private val commitments = FakeCommitmentRepository()
    private val detail = FakeEnvelopeDetailRepository(listOf(unassigned, food, trip, old))

    private fun period(index: Int, assigned: Long, activity: Long) = EnvelopePeriod(
        periodId = "p$index",
        startDate = LocalDate(2026, index, 1),
        endDate = LocalDate(2026, index, 28),
        openingCents = 1_000L * index,
        assignedCents = assigned,
        activityCents = activity,
        closingCents = 1_000L * index + assigned + activity,
    )

    private fun transfer(index: Int, origin: TransferOrigin = TransferOrigin.Manual, from: String = "e0", to: String = "e1", amount: Long = 1_000) =
        EnvelopeTransfer(
            id = "t$index",
            fromEnvelopeId = from,
            toEnvelopeId = to,
            amountCents = amount,
            origin = origin,
            createdAt = Instant.parse("2026-11-14T10:%02d:00Z".format(59 - index % 60)),
            commitmentId = if (origin == TransferOrigin.Commitment) "c1" else null,
            commitmentName = if (origin == TransferOrigin.Commitment) "Insurance" else null,
        )

    private fun movement(index: Int, amount: Long = -2_000) = Movement(
        id = "m$index", source = MovementSource.Transaction, accountId = "a1", counterAccountId = null, envelopeId = "e1",
        kind = MovementKind.Expense, amountCents = amount, date = LocalDate(2026, 11, 14), notes = "Lunch $index",
        createdBy = "u1", createdAt = "2026-11-14T10:00:00Z",
    )

    private fun viewModel(envelopeId: String = "e1") = EnvelopeDetailViewModel(
        groupId = "g1",
        envelopeId = envelopeId,
        groups = groups,
        envelopes = envelopes,
        members = members,
        accounts = accounts,
        movements = ledger,
        commitments = commitments,
        detail = detail,
    )

    private fun loaded(envelopeId: String = "e1"): EnvelopeDetailViewModel =
        viewModel(envelopeId).also { it.onEvent(EnvelopeDetailEvent.Refresh) }

    @Test
    fun anEnvelopeLoadsWithItsPencilAndHolder() {
        val state = loaded().state.value

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(food, state.envelope)
        assertEquals("Home", state.groupName)
        assertEquals("EUR", state.currencyCode)
        assertEquals("Ana", state.holderName)
        assertTrue(state.hasSeveralMembers)
        assertTrue(state.hasSeveralAccounts)
        assertTrue(state.canEditEnvelope)
        assertFalse(state.isUnassigned)
    }

    @Test
    fun unassignedIsReadOnlyWithNoOutlookAndNoCommitments() {
        detail.outlook = EnvelopeOutlook(25_000, 0, 25_000, 0)
        detail.fundingCommitments = listOf(FundingCommitment("c1", "Insurance", "a1", CommitmentKind.Expense))
        val state = loaded("e0").state.value

        assertTrue(state.isUnassigned)
        assertFalse(state.canEditEnvelope)
        assertNull(state.outlook)
        assertTrue(state.funding.isEmpty())
        assertFalse("getOutlook(g1, e0)" in detail.calls)
        assertFalse("getFundingCommitments(e0)" in detail.calls)
        // Its figures still come from the database: the period tile and the moves
        assertEquals(25_000, state.envelope?.balanceCents)
    }

    @Test
    fun anArchivedEnvelopeOpensWithoutThePencilOrTheOutlook() {
        val state = loaded("e3").state.value

        assertEquals(old, state.envelope)
        assertFalse(state.canEditEnvelope)
        assertNull(state.outlook)
        assertFalse("getOutlook(g1, e3)" in detail.calls)
    }

    @Test
    fun aMemberDoesNotSeeThePencil() {
        groups.groups = listOf(Group("g1", "Home", "EUR", GroupRole.Member))

        assertFalse(loaded().state.value.canEditEnvelope)
    }

    @Test
    fun theCurrentPeriodRowIsTheOneOfToday() {
        // The fake ledger's today is 2026-11-14; the periods of the fake end on the 28th
        detail.periods = listOf(period(9, 40_000, -30_000), period(10, 40_000, -41_000), period(11, 40_000, -33_950))
        val state = loaded().state.value

        assertEquals("p11", state.period?.periodId)
        assertEquals(11_000, state.period?.openingCents)
        assertEquals(17_050, state.period?.closingCents)
        assertEquals(40_000, state.period?.assignedCents)
        assertEquals(-33_950, state.period?.activityCents)
        assertEquals(3, state.chartPeriods.size)
    }

    @Test
    fun withoutAPeriodOfTodayTheNewestOneIsShown() {
        detail.periods = listOf(period(9, 1, -1), period(10, 2, -2))
        val state = loaded().state.value

        assertEquals("p10", state.period?.periodId)
    }

    @Test
    fun noPeriodsLeavesTheTileOut() {
        val state = loaded().state.value

        assertNull(state.period)
        assertTrue(state.chartPeriods.isEmpty())
    }

    @Test
    fun theChartAsksForTheLastSixPeriodsAndPlotsAssignedAndSpent() {
        detail.periods = (1..8).map { period(it, assigned = 10_000L * it, activity = -1_000L * it) }
        val state = loaded().state.value

        assertTrue("getPeriods(e1, 6)" in detail.calls)
        assertEquals(6, state.chartPeriods.size)
        assertEquals(listOf(30_000L, 3_000L), state.chartPeriods.first().chartColumns())
    }

    @Test
    fun chartColumnsNeverGoBelowZero() {
        // Income inside the period makes the activity positive; a net outgoing assignment is negative
        assertEquals(listOf(0L, 0L), period(1, assigned = -500, activity = 700).chartColumns())
        assertEquals(listOf(2_000L, 0L), period(1, assigned = 2_000, activity = 0).chartColumns())
    }

    @Test
    fun theOutlookCarriesTheCommitmentsAndTheShortfall() {
        detail.outlook = EnvelopeOutlook(balanceCents = 12_050, committedCents = 20_000, freeCents = -7_950, shortfallCents = 7_950)
        val state = loaded().state.value

        assertEquals(EnvelopeOutlook(12_050, 20_000, -7_950, 7_950), state.outlook)
        assertTrue("getOutlook(g1, e1)" in detail.calls)
    }

    @Test
    fun onlyTheCommitmentsThatReserveMoneyForThisEnvelopeAreListed() {
        detail.fundingCommitments = listOf(
            FundingCommitment("c1", "Insurance", "a1", CommitmentKind.Expense),
            FundingCommitment("c2", "Old plan", "a2", CommitmentKind.Expense),
            FundingCommitment("c3", "Moved", "a1", CommitmentKind.Expense),
        )
        commitments.fundingById = mapOf(
            "c1" to CommitmentFunding("e1", LocalDate(2027, 3, 19), 25_100, LocalDate(2026, 11, 1), 5_020, 5_020, 5, 4),
            // Not funded in force any more
            "c2" to null,
            // Funds another envelope now
            "c3" to CommitmentFunding("e2", LocalDate(2027, 1, 1), 1_000, LocalDate(2026, 11, 1), 0, 0, 1, 0),
        )
        val funding = loaded().state.value.funding

        assertEquals(listOf(FundingRow("c1", "a1", "Insurance", CommitmentKind.Expense, 5_020, LocalDate(2027, 3, 19))), funding)
    }

    @Test
    fun noFundingCommitmentsLeavesTheListEmpty() {
        assertTrue(loaded().state.value.funding.isEmpty())
    }

    @Test
    fun movesAreSignedForThisEnvelopeAndNamedByTheOtherSide() {
        detail.transfers = listOf(
            transfer(1, TransferOrigin.Target, from = "e0", to = "e1", amount = 40_000),
            transfer(2, TransferOrigin.Manual, from = "e1", to = "e2", amount = 3_000),
            transfer(3, TransferOrigin.Commitment, from = "e0", to = "e1", amount = 5_000),
        )
        val state = loaded().state.value

        assertEquals(3, state.transfersTotal)
        val (target, manual, reserve) = state.transfers
        assertEquals(40_000, target.signedCents)
        assertTrue(target.otherIsUnassigned)
        assertNull(target.otherName)
        assertEquals(-3_000, manual.signedCents)
        assertEquals("Trip", manual.otherName)
        assertFalse(manual.otherIsUnassigned)
        assertEquals("Insurance", reserve.transfer.commitmentName)
        // Entered at 10:xx UTC on the 14th: the 14th in Madrid too
        assertEquals(LocalDate(2026, 11, 14), target.date)
    }

    @Test
    fun theDayOfAMoveIsReadInTheGroupTimeZone() {
        groups.groups = listOf(Group("g1", "Home", "EUR", GroupRole.Owner, timezone = "Pacific/Auckland"))
        detail.transfers = listOf(transfer(1).copy(createdAt = Instant.parse("2026-11-14T20:00:00Z")))
        val state = loaded().state.value

        // 20:00 UTC is already the 15th, 09:00, in Auckland
        assertEquals(LocalDate(2026, 11, 15), state.transfers.single().date)
    }

    @Test
    fun manualMovesTargetsAndReservesCanBeDeletedButNotCoversOrReleases() {
        detail.transfers = listOf(
            transfer(1, TransferOrigin.Manual),
            transfer(2, TransferOrigin.Target),
            transfer(3, TransferOrigin.Commitment),
            transfer(4, TransferOrigin.Cover),
            transfer(5, TransferOrigin.Release),
        )
        val state = loaded().state.value

        assertEquals(listOf(true, true, true, false, false), state.transfers.map { it.canDelete })
        assertEquals(listOf(true, true, false, false, false), state.transfers.map { it.canEdit })
        assertTrue(state.canWriteTransfers)
    }

    @Test
    fun deletingAMonthlyTargetVoidsItWhenTheSnackbarEnds() {
        detail.transfers = listOf(transfer(1, TransferOrigin.Target, amount = 40_000))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t1"))

        assertTrue(viewModel.state.value.transfers.isEmpty())
        viewModel.onEvent(EnvelopeDetailEvent.UndoExpired)

        assertEquals(listOf("t1"), detail.voided)
    }

    @Test
    fun deletingACoverDoesNothing() {
        detail.transfers = listOf(transfer(1, TransferOrigin.Cover))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t1"))

        assertEquals(1, viewModel.state.value.transfers.size)
        assertNull(viewModel.state.value.undoMessage)
        assertTrue(detail.voided.isEmpty())
    }

    @Test
    fun deletingAManualMoveHidesItAtOnceAndVoidsItWhenTheSnackbarEnds() {
        detail.transfers = listOf(transfer(1), transfer(2))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t1"))

        val state = viewModel.state.value
        assertEquals(listOf("t2"), state.transfers.map { it.transfer.id })
        assertEquals(1, state.transfersTotal)
        assertEquals(Res.string.transfer_deleted, state.undoMessage?.message)
        assertEquals(1_000L, state.undoMessage?.amountCents)
        // Nothing is written while the snackbar is up
        assertTrue(detail.voided.isEmpty())

        viewModel.onEvent(EnvelopeDetailEvent.UndoExpired)

        assertEquals(listOf("t1"), detail.voided)
        assertNull(viewModel.state.value.undoMessage)
        assertEquals(listOf("t2"), viewModel.state.value.transfers.map { it.transfer.id })
        assertEquals(1, viewModel.state.value.transfersTotal)
    }

    @Test
    fun undoBringsTheMoveBackWithoutWritingAnything() {
        detail.transfers = listOf(transfer(1), transfer(2), transfer(3))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t2"))
        viewModel.onEvent(EnvelopeDetailEvent.UndoClicked)

        assertTrue(detail.voided.isEmpty())
        assertEquals(listOf("t1", "t2", "t3"), viewModel.state.value.transfers.map { it.transfer.id })
        assertEquals(3, viewModel.state.value.transfersTotal)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun aFailedVoidBringsTheMoveBackWithTheError() {
        detail.transfers = listOf(transfer(1))
        detail.voidResult = Result.failure(AppError.BusinessRule("FZ016"))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t1"))
        viewModel.onEvent(EnvelopeDetailEvent.UndoExpired)

        assertEquals(listOf("t1"), viewModel.state.value.transfers.map { it.transfer.id })
        assertEquals(1, viewModel.state.value.transfersTotal)
        assertEquals(AppError.BusinessRule("FZ016"), viewModel.state.value.actionError)
    }

    @Test
    fun aPendingDeleteIsFinalBeforeTheScreenIsReadAgain() {
        detail.transfers = listOf(transfer(1), transfer(2))
        val viewModel = loaded()
        viewModel.onEvent(EnvelopeDetailEvent.DeleteTransferClicked("t1"))
        viewModel.onEvent(EnvelopeDetailEvent.Refresh)

        assertEquals(listOf("t1"), detail.voided)
        assertEquals(listOf("t2"), viewModel.state.value.transfers.map { it.transfer.id })
    }

    @Test
    fun movesArePagedTwentyAtATimeWithTheExactCount() {
        detail.transfers = (1..45).map { transfer(it) }
        val viewModel = loaded()

        assertTrue("getTransfers(e1, 0, 20)" in detail.calls)
        assertEquals(20, viewModel.state.value.transfers.size)
        assertEquals(45, viewModel.state.value.transfersTotal)
        assertTrue(viewModel.state.value.hasMoreTransfers)

        viewModel.onEvent(EnvelopeDetailEvent.LoadMoreTransfers)
        assertTrue("getTransfers(e1, 20, 20)" in detail.calls)
        assertEquals(40, viewModel.state.value.transfers.size)

        viewModel.onEvent(EnvelopeDetailEvent.LoadMoreTransfers)
        assertEquals(45, viewModel.state.value.transfers.size)
        assertFalse(viewModel.state.value.hasMoreTransfers)

        // Nothing left: no further request
        val calls = detail.calls.size
        viewModel.onEvent(EnvelopeDetailEvent.LoadMoreTransfers)
        assertEquals(calls, detail.calls.size)
    }

    @Test
    fun aFailedPageCanBeRetried() {
        detail.transfers = (1..30).map { transfer(it) }
        val viewModel = loaded()
        detail.transfersResult = Result.failure(AppError.Network)
        viewModel.onEvent(EnvelopeDetailEvent.LoadMoreTransfers)

        assertTrue(viewModel.state.value.loadMoreTransfersFailed)
        assertEquals(20, viewModel.state.value.transfers.size)

        detail.transfersResult = null
        viewModel.onEvent(EnvelopeDetailEvent.LoadMoreTransfers)

        assertFalse(viewModel.state.value.loadMoreTransfersFailed)
        assertEquals(30, viewModel.state.value.transfers.size)
    }

    @Test
    fun theLatestFiveMovementsOfTheEnvelopeAreRead() {
        ledger.all = (1..8).map { movement(it) }
        val state = loaded().state.value

        val call = ledger.searches.last()
        assertEquals("e1", call.filter.envelopeId)
        assertEquals(5, call.limit)
        assertEquals(0, call.offset)
        assertEquals(5, state.latestMovements.size)
        assertEquals("Bank", state.accountNames["a1"])
    }

    @Test
    fun eventsOfTheRouteLeaveTheStateAlone() {
        val viewModel = loaded()
        val before = viewModel.state.value
        viewModel.onEvent(EnvelopeDetailEvent.SeeAllClicked)
        viewModel.onEvent(EnvelopeDetailEvent.EditClicked)
        viewModel.onEvent(EnvelopeDetailEvent.BackClicked)
        viewModel.onEvent(EnvelopeDetailEvent.CommitmentClicked("a1", "c1"))

        assertEquals(before, viewModel.state.value)
    }

    @Test
    fun anEnvelopeThatIsNotThereShowsNotFound() {
        val state = loaded("nope").state.value

        assertEquals(AppError.NotFound, state.error)
        assertFalse(state.isLoading)
        assertNull(state.envelope)
    }

    @Test
    fun aFailureOfAFigureShowsTheErrorAndRetryReloads() {
        detail.periodsResult = Result.failure(AppError.Network)
        val viewModel = loaded()

        assertEquals(AppError.Network, viewModel.state.value.error)

        detail.periodsResult = null
        viewModel.onEvent(EnvelopeDetailEvent.Retry)

        assertNull(viewModel.state.value.error)
        assertEquals(food, viewModel.state.value.envelope)
    }

    @Test
    fun aRefreshThatFailsKeepsWhatIsOnScreen() {
        val viewModel = loaded()
        detail.periodsResult = Result.failure(AppError.Network)
        viewModel.onEvent(EnvelopeDetailEvent.Refresh)

        assertNull(viewModel.state.value.error)
        assertEquals(food, viewModel.state.value.envelope)
        assertEquals(AppError.Network, viewModel.state.value.actionError)
    }

    @Test
    fun aGroupWithOneMemberAndOneAccountHidesHolderAndAccountLines() {
        members.members = listOf(ana)
        accounts.accounts = listOf(bank)
        val state = loaded().state.value

        assertFalse(state.hasSeveralMembers)
        assertFalse(state.hasSeveralAccounts)
    }
}
