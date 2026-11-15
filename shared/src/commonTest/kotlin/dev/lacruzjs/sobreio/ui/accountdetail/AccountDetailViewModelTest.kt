package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.adjustment_recorded
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest : MainDispatcherTest() {

    private val bank = Account("a1", "g1", "Bank", AccountKind.Bank, true, 100_000, ownerUserId = "u1")
    private val cash = Account("a2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val ana = Member("u1", "Ana", GroupRole.Owner)
    private val leo = Member("u2", "Leo", GroupRole.Member)
    private val unassigned = Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 0)
    private val food = Envelope("e1", "g1", "Food", EnvelopeKind.Spending, 0, 0)

    private val groups = FakeGroupRepository(listOf(Group("g1", "Home", "EUR", GroupRole.Owner)))
    private val accounts = FakeAccountRepository(listOf(bank, cash))
    private val members = FakeMemberRepository(listOf(ana, leo))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food))
    private val detail = FakeAccountDetailRepository()

    private val viewModel by lazy {
        AccountDetailViewModel("g1", "a1", groups, accounts, members, envelopes, detail, debounceMillis = 300)
    }

    private fun loaded(): AccountDetailViewModel = viewModel.also { it.onEvent(AccountDetailEvent.Refresh) }

    private fun settle() = testDispatcher.scheduler.advanceUntilIdle()

    @Test
    fun refreshLoadsTheAccountAndEverythingAroundIt() {
        detail.movements = listOf(
            AccountMovement("m1", MovementKind.Expense, -2_000, LocalDate(2026, 11, 13), "Lunch", null, "e1", "u2"),
            AccountMovement("m2", MovementKind.TransferOut, -5_000, LocalDate(2026, 11, 12), null, "a2", null, "u1"),
            AccountMovement("m3", MovementKind.Income, 9_000, LocalDate(2026, 11, 11), null, null, "e0", "gone"),
        )
        loaded()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(bank, state.account)
        assertEquals("Home", state.groupName)
        assertEquals("EUR", state.currencyCode)
        assertTrue(state.canEdit)
        assertEquals(LocalDate(2026, 11, 14), state.today)
        assertEquals(LocalDate(2026, 11, 30), state.periodEnd)
        assertEquals("Ana", state.holderName)
        assertTrue(state.hasSeveralMembers)
        assertEquals(detail.series, state.balanceSeries)
        val (lunch, transfer, income) = state.movements
        assertEquals("Leo", lunch.authorName)
        assertEquals("Food", lunch.envelopeName)
        assertEquals("Cash", transfer.counterAccountName)
        assertEquals("Ana", transfer.authorName)
        assertTrue(income.envelopeIsUnassigned)
        assertNull(income.envelopeName)
        assertNull(income.authorName)
    }

    @Test
    fun theBalanceSeriesStartsThreeMonthsBeforeTodayByDefault() {
        loaded()
        assertTrue("getBalanceSeries(2026-08-14, 2026-11-14)" in detail.calls)
    }

    @Test
    fun anUnknownAccountIsNotFound() {
        accounts.accounts = listOf(cash)
        loaded()

        assertEquals(AppError.NotFound, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun aFailedLoadShowsTheErrorAndRetryRecovers() {
        detail.todayResult = Result.failure(AppError.Network)
        loaded()
        assertEquals(AppError.Network, viewModel.state.value.error)

        detail.todayResult = null
        viewModel.onEvent(AccountDetailEvent.Retry)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.account)
    }

    @Test
    fun changingTheRangeReadsTheSeriesAgainOnlyForThatRange() {
        loaded()
        detail.series = detail.series.take(1)
        viewModel.onEvent(AccountDetailEvent.RangeSelected(BalanceRange.OneYear))

        assertEquals(BalanceRange.OneYear, viewModel.state.value.range)
        assertTrue("getBalanceSeries(2025-11-14, 2026-11-14)" in detail.calls)
        assertEquals(1, viewModel.state.value.balanceSeries.size)
        assertFalse(viewModel.state.value.isSeriesLoading)
        // The forecast was not asked for again
        assertEquals(1, detail.calls.count { it.startsWith("getForecast") })
    }

    @Test
    fun aFailedRangeChangeKeepsTheOldSeriesAndShowsTheError() {
        loaded()
        val before = viewModel.state.value.balanceSeries
        detail.seriesResult = Result.failure(AppError.Network)
        viewModel.onEvent(AccountDetailEvent.RangeSelected(BalanceRange.SixMonths))

        assertEquals(before, viewModel.state.value.balanceSeries)
        assertEquals(AppError.Network, viewModel.state.value.actionError)
    }

    @Test
    fun aShortfallGetsTheDateOfTheLowestPoint() {
        detail.forecast = AccountForecast(100_000, 0, 150_000, -50_000, -50_000, 50_000)
        detail.projection = listOf(
            ProjectionPoint(LocalDate(2026, 11, 14), 100_000, 0),
            ProjectionPoint(LocalDate(2026, 11, 20), -50_000, -150_000),
            ProjectionPoint(LocalDate(2026, 11, 25), -50_000, 0),
        )
        loaded()

        assertEquals(LocalDate(2026, 11, 20), viewModel.state.value.shortfallDate)
    }

    @Test
    fun noShortfallMeansNoDate() {
        detail.projection = listOf(ProjectionPoint(LocalDate(2026, 11, 14), 100_000, 0))
        loaded()

        assertNull(viewModel.state.value.shortfallDate)
    }

    @Test
    fun anArchivedAccountHasNoForecast() {
        detail.forecast = null
        loaded()

        assertNull(viewModel.state.value.forecast)
        assertNull(viewModel.state.value.shortfallDate)
    }

    @Test
    fun onlyOwnersAndAdminsCanEdit() {
        groups.groups = listOf(Group("g1", "Home", "EUR", GroupRole.Member))
        loaded()
        assertFalse(viewModel.state.value.canEdit)
    }

    @Test
    fun adminsCanEdit() {
        groups.groups = listOf(Group("g1", "Home", "EUR", GroupRole.Admin))
        loaded()
        assertTrue(viewModel.state.value.canEdit)
    }

    @Test
    fun aSingleMemberGroupHasNoSeveralMembers() {
        members.members = listOf(ana)
        loaded()
        assertFalse(viewModel.state.value.hasSeveralMembers)
    }

    @Test
    fun comparingWaitsForTypingToPauseAndAsksOnlyForTheLastValue() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1"))
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("12"))
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1200,50"))

        assertTrue(viewModel.state.value.compare!!.isComparing)
        assertTrue(detail.calls.none { it.startsWith("compareWithBank") })

        testDispatcher.scheduler.advanceTimeBy(301)
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("compareWithBank(120050)"), detail.calls.filter { it.startsWith("compareWithBank") })
        val compare = viewModel.state.value.compare!!
        assertFalse(compare.isComparing)
        assertEquals(100_000, compare.result?.appBalanceCents)
        assertEquals(20_050, compare.result?.differenceCents)
    }

    @Test
    fun anInvalidAmountShowsAMessageAndAsksNothing() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("12x"))
        settle()

        assertEquals(Res.string.validation_amount_invalid, viewModel.state.value.compare?.inputError)
        assertNull(viewModel.state.value.compare?.result)
        assertTrue(detail.calls.none { it.startsWith("compareWithBank") })
    }

    @Test
    fun aBlankOrLoneMinusInputClearsTheResultWithoutAMessage() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1000"))
        settle()
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged(""))
        settle()

        assertNull(viewModel.state.value.compare?.result)
        assertNull(viewModel.state.value.compare?.inputError)
        assertTrue(detail.calls.count { it.startsWith("compareWithBank") } == 1)
    }

    @Test
    fun aBankBalanceCanBeNegative() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("20"))
        viewModel.onEvent(AccountDetailEvent.BankSignChanged(true))
        settle()

        assertTrue("compareWithBank(-2000)" in detail.calls)
        assertTrue(viewModel.state.value.compare!!.bankIsNegative)
    }

    @Test
    fun changingTheSignComparesAgain() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        assertFalse(viewModel.state.value.compare!!.bankIsNegative)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("20"))
        settle()
        viewModel.onEvent(AccountDetailEvent.BankSignChanged(true))
        settle()

        assertEquals(listOf("compareWithBank(2000)", "compareWithBank(-2000)"), detail.calls.filter { it.startsWith("compareWithBank") })
    }

    @Test
    fun aMatchingBalanceHasNothingToAdjust() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1000"))
        settle()
        viewModel.onEvent(AccountDetailEvent.AdjustClicked)

        assertEquals(0, viewModel.state.value.compare?.result?.differenceCents)
        assertTrue(detail.calls.none { it.startsWith("recordAdjustment") })
    }

    @Test
    fun adjustingRecordsTheDifferenceClosesTheSheetReloadsAndOffersUndo() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1200"))
        settle()
        val callsBefore = detail.calls.count { it.startsWith("getForecast") }
        viewModel.onEvent(AccountDetailEvent.AdjustClicked)

        assertTrue("recordAdjustment(20000)" in detail.calls)
        val state = viewModel.state.value
        assertNull(state.compare)
        assertEquals(callsBefore + 1, detail.calls.count { it.startsWith("getForecast") })
        assertEquals(Res.string.adjustment_recorded, state.undoMessage?.message)
        assertEquals(20_000L, state.undoMessage?.amountCents)
        assertTrue(state.undoMessage?.undoable == true)
    }

    @Test
    fun undoVoidsTheAdjustmentAndReloads() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("800"))
        settle()
        viewModel.onEvent(AccountDetailEvent.AdjustClicked)
        val loadsBefore = detail.calls.count { it.startsWith("getForecast") }
        viewModel.onEvent(AccountDetailEvent.UndoClicked)

        assertTrue("recordAdjustment(-20000)" in detail.calls)
        assertTrue("voidTransaction(tx-1)" in detail.calls)
        assertEquals(loadsBefore + 1, detail.calls.count { it.startsWith("getForecast") })
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun aFailedAdjustmentKeepsTheSheetOpenWithTheError() {
        loaded()
        detail.adjustmentResult = Result.failure(AppError.BusinessRule("FZ001"))
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1200"))
        settle()
        viewModel.onEvent(AccountDetailEvent.AdjustClicked)

        val compare = viewModel.state.value.compare
        assertNotNull(compare)
        assertEquals(AppError.BusinessRule("FZ001"), compare.error)
        assertFalse(compare.isAdjusting)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun dismissingTheSheetDropsAPendingComparison() {
        loaded()
        viewModel.onEvent(AccountDetailEvent.CompareClicked)
        viewModel.onEvent(AccountDetailEvent.BankBalanceChanged("1200"))
        viewModel.onEvent(AccountDetailEvent.CompareDismissed)
        settle()

        assertNull(viewModel.state.value.compare)
        assertTrue(detail.calls.none { it.startsWith("compareWithBank") })
    }

    @Test
    fun aRefreshAfterTheFirstLoadKeepsTheContentOnScreen() {
        loaded()
        detail.todayResult = Result.failure(AppError.Network)
        viewModel.onEvent(AccountDetailEvent.Refresh)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.account)
        assertEquals(AppError.Network, state.actionError)
    }
}
