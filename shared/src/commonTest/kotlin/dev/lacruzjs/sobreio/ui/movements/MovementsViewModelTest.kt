package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.MovementType
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.fake.FakeMovementRepository
import dev.lacruzjs.sobreio.fake.FakeTagRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.movement_deleted
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest : MainDispatcherTest() {

    private val main = Account("a1", "g1", "Main", AccountKind.Bank, true, 0)
    private val savings = Account("a2", "g1", "Savings", AccountKind.Savings, true, 0)
    private val ana = Member("u1", "Ana", GroupRole.Owner)
    private val luis = Member("u2", "Luis", GroupRole.Member)

    private fun movement(index: Int, date: LocalDate = LocalDate(2026, 11, 14), amount: Long = -1000L) = Movement(
        id = "m$index", source = MovementSource.Transaction, accountId = "a1", counterAccountId = null, envelopeId = "e1",
        kind = MovementKind.Expense, amountCents = amount, date = date, notes = null, createdBy = "u1",
        createdAt = "2026-11-14T10:%02d:00+00:00".format(59 - index % 60),
    )

    private val ledger = FakeMovementRepository((1..3).map { movement(it) })
    private val groups = FakeGroupRepository(listOf(Group("g1", "Casa", "USD", role = GroupRole.Owner)))
    private val accounts = FakeAccountRepository(listOf(main, savings))
    private val members = FakeMemberRepository(listOf(ana, luis))

    private fun viewModel(fixedAccountId: String? = null) = MovementsViewModel(
        groupId = "g1",
        fixedAccountId = fixedAccountId,
        movements = ledger,
        groups = groups,
        accounts = accounts,
        envelopes = FakeEnvelopeRepository(),
        members = members,
        tags = FakeTagRepository(),
    )

    private fun loaded(fixedAccountId: String? = null): MovementsViewModel =
        viewModel(fixedAccountId).also { it.onEvent(MovementsEvent.Refresh) }

    @Test
    fun refreshLoadsTheFirstPageWithItsTotalAndTheNames() {
        val viewModel = loaded()

        val state = viewModel.state.value
        assertEquals(3, state.items.size)
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
        assertEquals("Casa", state.groupName)
        assertEquals("USD", state.currencyCode)
        assertTrue(state.canEdit)
        assertEquals(listOf("Main", "Savings"), state.accounts.map { it.name })
        assertEquals(2, state.members.size)
        assertEquals(SearchCallOf(MovementsViewModel.PAGE_SIZE, 0, true), ledger.searches.last().let { SearchCallOf(it.limit, it.offset, it.withTotal) })
    }

    private data class SearchCallOf(val limit: Int, val offset: Int, val withTotal: Boolean)

    @Test
    fun anAccountFixedByTheRouteIsAlwaysInTheFilter() {
        val viewModel = loaded(fixedAccountId = "a1")

        assertEquals("a1", ledger.searches.last().filter.accountId)
        assertEquals(main, viewModel.state.value.fixedAccount)

        viewModel.onEvent(MovementsEvent.ChipCleared(MovementsChip.Account))
        viewModel.onEvent(MovementsEvent.TypeSelected(MovementType.Income))

        assertEquals("a1", ledger.searches.last().filter.accountId)
        assertTrue(viewModel.state.value.hasActiveFilters)
        viewModel.onEvent(MovementsEvent.ChipCleared(MovementsChip.Type))
        assertFalse(viewModel.state.value.hasActiveFilters)
    }

    @Test
    fun everyFilterGoesToTheRepository() {
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.AccountSelected("a2"))
        viewModel.onEvent(MovementsEvent.EnvelopeSelected("e1"))
        viewModel.onEvent(MovementsEvent.TypeSelected(MovementType.Transfer))
        viewModel.onEvent(MovementsEvent.TagsApplied(setOf("t1", "t2")))
        viewModel.onEvent(MovementsEvent.PersonSelected("u2"))

        val filter = ledger.searches.last().filter
        assertEquals("a2", filter.accountId)
        assertEquals("e1", filter.envelopeId)
        assertEquals(MovementType.Transfer, filter.type)
        assertEquals(setOf("t1", "t2"), filter.tagIds)
        assertEquals("u2", filter.createdBy)
        assertTrue(viewModel.state.value.hasActiveFilters)
        assertEquals(MovementsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun clearingAChipRemovesOnlyThatFilter() {
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.AccountSelected("a2"))
        viewModel.onEvent(MovementsEvent.TypeSelected(MovementType.Expense))

        viewModel.onEvent(MovementsEvent.ChipCleared(MovementsChip.Account))

        val filter = ledger.searches.last().filter
        assertNull(filter.accountId)
        assertEquals(MovementType.Expense, filter.type)
    }

    @Test
    fun datePresetsBecomeInclusiveDates() {
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.DatePresetSelected(DatePreset.PreviousPeriod))

        val filter = ledger.searches.last().filter
        assertEquals(LocalDate(2026, 10, 1), filter.from)
        assertEquals(LocalDate(2026, 10, 31), filter.to)
        assertEquals(DatePreset.PreviousPeriod, viewModel.state.value.datePreset)

        viewModel.onEvent(MovementsEvent.ChipCleared(MovementsChip.Dates))

        assertNull(ledger.searches.last().filter.from)
        assertNull(viewModel.state.value.datePreset)
    }

    @Test
    fun aCustomRangeOpensThePickerAndAppliesWhenChosen() {
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.DatePresetSelected(DatePreset.Custom))
        assertTrue(viewModel.state.value.customRangeOpen)

        viewModel.onEvent(MovementsEvent.CustomRangeSelected(LocalDate(2026, 5, 1), LocalDate(2026, 5, 20)))

        assertFalse(viewModel.state.value.customRangeOpen)
        assertEquals(LocalDate(2026, 5, 1), ledger.searches.last().filter.from)
        assertEquals(LocalDate(2026, 5, 20), ledger.searches.last().filter.to)
        assertEquals(DatePreset.Custom, viewModel.state.value.datePreset)
    }

    @Test
    fun searchWaitsForTheTypingToPause() {
        val viewModel = loaded()
        val before = ledger.searches.size

        viewModel.onEvent(MovementsEvent.SearchOpened)
        viewModel.onEvent(MovementsEvent.SearchChanged("re"))
        testDispatcher.scheduler.advanceTimeBy(200)
        viewModel.onEvent(MovementsEvent.SearchChanged("rent"))
        testDispatcher.scheduler.advanceTimeBy(200)

        assertEquals(before, ledger.searches.size)
        assertEquals("rent", viewModel.state.value.searchText)

        testDispatcher.scheduler.advanceTimeBy(150)

        assertEquals(before + 1, ledger.searches.size)
        assertEquals("rent", ledger.searches.last().filter.query)
    }

    @Test
    fun closingTheSearchClearsTheTextAndTheQuery() {
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.SearchOpened)
        viewModel.onEvent(MovementsEvent.SearchChanged("rent"))
        testDispatcher.scheduler.advanceTimeBy(400)
        assertEquals("rent", ledger.searches.last().filter.query)

        viewModel.onEvent(MovementsEvent.SearchClosed)

        assertFalse(viewModel.state.value.searchOpen)
        assertEquals("", viewModel.state.value.searchText)
        assertEquals("", ledger.searches.last().filter.query)
    }

    @Test
    fun closingTheSearchBeforeTheDebounceNeverAsks() {
        val viewModel = loaded()
        val before = ledger.searches.size
        viewModel.onEvent(MovementsEvent.SearchOpened)
        viewModel.onEvent(MovementsEvent.SearchChanged("rent"))

        viewModel.onEvent(MovementsEvent.SearchClosed)
        testDispatcher.scheduler.advanceTimeBy(1000)

        assertEquals(before, ledger.searches.size)
    }

    @Test
    fun pagesAreRequestedFromWhereTheLastEnded() {
        ledger.all = (1..120).map { movement(it) }
        val viewModel = loaded()
        assertEquals(50, viewModel.state.value.items.size)
        assertTrue(viewModel.state.value.hasMore)

        viewModel.onEvent(MovementsEvent.LoadMore)
        assertEquals(100, viewModel.state.value.items.size)
        assertEquals(50, ledger.searches.last().offset)
        assertFalse(ledger.searches.last().withTotal)

        viewModel.onEvent(MovementsEvent.LoadMore)
        assertEquals(120, viewModel.state.value.items.size)
        assertFalse(viewModel.state.value.hasMore)

        val calls = ledger.searches.size
        viewModel.onEvent(MovementsEvent.LoadMore)
        assertEquals(calls, ledger.searches.size)
    }

    @Test
    fun aFailedPageCanBeRetried() {
        ledger.all = (1..120).map { movement(it) }
        val viewModel = loaded()
        ledger.searchResult = Result.failure(AppError.Network)

        viewModel.onEvent(MovementsEvent.LoadMore)

        assertTrue(viewModel.state.value.loadMoreFailed)
        assertEquals(50, viewModel.state.value.items.size)

        ledger.searchResult = Result.success(Unit)
        viewModel.onEvent(MovementsEvent.RetryLoadMore)

        assertFalse(viewModel.state.value.loadMoreFailed)
        assertEquals(100, viewModel.state.value.items.size)
    }

    @Test
    fun aFailedFirstPageShowsTheError() {
        ledger.searchResult = Result.failure(AppError.Network)

        val viewModel = loaded()

        assertEquals(AppError.Network, viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun deleteHidesTheRowAndOnlyVoidsWhenTheSnackbarEnds() {
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.DeleteClicked("m2"))

        assertEquals(listOf("m1", "m3"), viewModel.state.value.items.map { it.id })
        assertEquals(Res.string.movement_deleted, viewModel.state.value.undoMessage?.message)
        assertEquals(1000L, viewModel.state.value.undoMessage?.amountCents)
        assertTrue(ledger.voided.isEmpty())

        viewModel.onEvent(MovementsEvent.UndoExpired)

        assertEquals(listOf("m2"), ledger.voided)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun undoBringsTheRowBackInPlaceAndNeverVoids() {
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.DeleteClicked("m2"))

        viewModel.onEvent(MovementsEvent.UndoClicked)

        assertEquals(listOf("m1", "m2", "m3"), viewModel.state.value.items.map { it.id })
        assertTrue(ledger.voided.isEmpty())
    }

    @Test
    fun aSecondDeleteMakesTheFirstFinal() {
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.DeleteClicked("m1"))

        viewModel.onEvent(MovementsEvent.DeleteClicked("m2"))

        assertEquals(listOf("m1"), ledger.voided)
        assertEquals(listOf("m3"), viewModel.state.value.items.map { it.id })
    }

    @Test
    fun changingTheFilterMakesAPendingDeleteFinalFirst() {
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.DeleteClicked("m1"))

        viewModel.onEvent(MovementsEvent.TypeSelected(MovementType.Expense))

        assertEquals(listOf("m1"), ledger.voided)
        assertEquals(listOf("m2", "m3"), viewModel.state.value.items.map { it.id })
    }

    @Test
    fun aFailedVoidBringsTheRowBackWithTheError() {
        val viewModel = loaded()
        ledger.voidResult = Result.failure(AppError.Network)
        viewModel.onEvent(MovementsEvent.DeleteClicked("m2"))

        viewModel.onEvent(MovementsEvent.UndoExpired)

        assertEquals(listOf("m1", "m2", "m3"), viewModel.state.value.items.map { it.id })
        assertEquals(AppError.Network, viewModel.state.value.actionError)
    }

    @Test
    fun theNextPageSkipsTheRowsAlreadyVoided() {
        ledger.all = (1..120).map { movement(it) }
        val viewModel = loaded()
        viewModel.onEvent(MovementsEvent.DeleteClicked("m1"))
        viewModel.onEvent(MovementsEvent.UndoExpired)

        viewModel.onEvent(MovementsEvent.LoadMore)

        // 49 rows were fetched and still count: the database now holds 119, and the next row is its 50th
        assertEquals(49, ledger.searches.last().offset)
        assertEquals(99, viewModel.state.value.items.size)
    }

    @Test
    fun contributionLegsCannotBeDeleted() {
        ledger.all = listOf(movement(1).copy(kind = MovementKind.GroupTransferOut))
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.DeleteClicked("m1"))

        assertEquals(1, viewModel.state.value.items.size)
        assertNull(viewModel.state.value.undoMessage)
    }

    @Test
    fun theNewButtonOpensTheComingSoonSheet() {
        val viewModel = loaded()

        viewModel.onEvent(MovementsEvent.NewClicked)
        assertEquals(MovementsSheet.New, viewModel.state.value.sheet)

        viewModel.onEvent(MovementsEvent.NewDismissed)
        assertEquals(MovementsSheet.None, viewModel.state.value.sheet)
    }

    @Test
    fun clearAllFiltersKeepsTheFixedAccountAndTheSearch() {
        val viewModel = loaded(fixedAccountId = "a1")
        viewModel.onEvent(MovementsEvent.TypeSelected(MovementType.Income))
        viewModel.onEvent(MovementsEvent.SearchChanged("rent"))
        testDispatcher.scheduler.advanceTimeBy(400)

        viewModel.onEvent(MovementsEvent.ClearAllFilters)

        val filter = ledger.searches.last().filter
        assertEquals("a1", filter.accountId)
        assertNull(filter.type)
        assertEquals("rent", filter.query)
    }

    @Test
    fun rowsAreGroupedByTheGroupsToday() {
        ledger.all = listOf(movement(1), movement(2, LocalDate(2026, 11, 13)))
        val viewModel = loaded()

        val sections = viewModel.state.value.sections

        assertEquals(listOf(DayRelative.Today, DayRelative.Yesterday), sections.map { it.relative })
    }

    @Test
    fun theOwnerNameIsKnownOnlyWithSeveralMembers() {
        members.members = listOf(ana)
        val viewModel = loaded()

        assertFalse(viewModel.state.value.hasSeveralMembers)
    }
}
