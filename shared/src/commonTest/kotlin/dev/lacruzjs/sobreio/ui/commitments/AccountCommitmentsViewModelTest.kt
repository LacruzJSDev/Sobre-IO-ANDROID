package dev.lacruzjs.sobreio.ui.commitments

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeCommitmentRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.commitment_deleted
import dev.lacruzjs.sobreio.generated.resources.commitment_reopened
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountCommitmentsViewModelTest : MainDispatcherTest() {

    private val today = LocalDate(2026, 11, 14)

    private fun version(
        id: String,
        kind: CommitmentKind,
        account: String = "acc",
        to: String? = null,
        amount: Long = 10_000,
    ) = CommitmentVersion(
        id = "v-$id", commitmentId = id, effectiveFrom = LocalDate(2026, 1, 1), effectiveTo = null, kind = kind,
        amountCents = amount, accountId = account, toAccountId = to, firstDueDate = LocalDate(2026, 1, 5),
        isRecurring = true, everyN = 1, unit = FrequencyUnit.Month, anchorDay = 5,
    )

    private fun commitment(id: String, name: String, v: CommitmentVersion, archived: Boolean = false) =
        Commitment(id, "g1", name, archivedAt = if (archived) "2026-11-01T10:00:00Z" else null, versions = listOf(v))

    private val rent = commitment("rent", "Rent", version("rent", CommitmentKind.Expense, amount = 70_000))
    private val salary = commitment("salary", "Salary", version("salary", CommitmentKind.Income, amount = 200_000))
    private val saving = commitment("saving", "Saving", version("saving", CommitmentKind.Transfer, to = "other", amount = 5_000))
    private val incoming = commitment("incoming", "From other", version("incoming", CommitmentKind.Transfer, account = "other", to = "acc", amount = 3_000))
    private val gift = commitment("gift", "Gift", version("gift", CommitmentKind.GroupTransferOut, amount = 2_000))
    private val received = commitment("received", "Received", version("received", CommitmentKind.GroupTransferIn, amount = 1_000))
    private val old = commitment("old", "Old", version("old", CommitmentKind.Expense), archived = true)
    private val elsewhere = commitment("else", "Elsewhere", version("else", CommitmentKind.Expense, account = "other"))
    private val link = CommitmentLink("l1", "g1", "g2", "gift", "gift-in")

    private val repository = FakeCommitmentRepository(listOf(rent, salary, saving, incoming, gift, received, old, elsewhere))
        .also { it.links = listOf(link) }
    private val groups = FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Owner)))
    private val detail = FakeAccountDetailRepository()
    private val accountRepository = FakeAccountRepository(
        listOf(
            Account("acc", "g1", "Bank", AccountKind.Bank, true, 0),
            Account("other", "g1", "Cash", AccountKind.Cash, true, 0),
        ),
    )
    private val viewModel by lazy { AccountCommitmentsViewModel("g1", "acc", repository, groups, detail, accountRepository) }

    private fun loaded(): AccountCommitmentsViewModel = viewModel.also { it.onEvent(CommitmentsEvent.Refresh) }

    @Test
    fun groupsRowsInSectionsWithTheSignForThisAccount() {
        loaded()
        val state = viewModel.state.value

        assertEquals(
            listOf(
                CommitmentSection.Expenses, CommitmentSection.Incomes, CommitmentSection.Transfers,
                CommitmentSection.IncomingTransfers, CommitmentSection.Contributions, CommitmentSection.ReceivedContributions,
            ),
            state.sections.map { it.first },
        )
        fun signed(id: String) = state.rows.first { it.id == id }.signedCents
        assertEquals(-70_000, signed("rent"))
        assertEquals(200_000, signed("salary"))
        assertEquals(-5_000, signed("saving"))
        assertEquals(3_000, signed("incoming"))
        assertEquals(-2_000, signed("gift"))
        assertEquals(1_000, signed("received"))
        // Not touching this account, and archived
        assertFalse(state.rows.any { it.id == "else" || it.id == "old" })
        assertEquals(listOf("old"), state.archivedRows.map { it.id })
    }

    @Test
    fun onlyTheEditableRowsAreEditable() {
        loaded()
        val rows = viewModel.state.value.rows.associateBy { it.id }

        assertTrue(rows.getValue("rent").editable)
        assertTrue(rows.getValue("saving").editable)
        assertTrue(rows.getValue("gift").editable)
        assertFalse(rows.getValue("incoming").editable)
        assertFalse(rows.getValue("received").editable)
    }

    @Test
    fun aContributionWithoutAVisibleLinkIsReadOnly() {
        repository.links = emptyList()
        loaded()

        assertFalse(viewModel.state.value.rows.first { it.id == "gift" }.editable)
    }

    @Test
    fun membersOnlyRead() {
        groups.groups = listOf(Group("g1", "Casa", "EUR", GroupRole.Member))
        loaded()

        assertFalse(viewModel.state.value.canEdit)
        assertTrue(viewModel.state.value.rows.none { it.editable })
    }

    @Test
    fun nextDueComesFromTheUpcomingOccurrences() {
        detail.upcoming = listOf(
            UpcomingItem("rent", "Rent", LocalDate(2026, 12, 5), -70_000),
            UpcomingItem("rent", "Rent", LocalDate(2026, 11, 20), -70_000),
        )
        loaded()

        assertEquals(LocalDate(2026, 11, 20), viewModel.state.value.rows.first { it.id == "rent" }.nextDue)
        assertNull(viewModel.state.value.rows.first { it.id == "salary" }.nextDue)
    }

    @Test
    fun deleteArchivesAtOnceAndUndoUnarchives() {
        loaded()
        viewModel.onEvent(CommitmentsEvent.DeleteClicked("rent"))

        assertTrue("archiveCommitment(rent)" in repository.calls)
        assertTrue(viewModel.state.value.rows.none { it.id == "rent" })
        assertTrue(viewModel.state.value.archivedRows.any { it.id == "rent" })
        assertEquals(Res.string.commitment_deleted, viewModel.state.value.undoMessage?.message)

        viewModel.onEvent(CommitmentsEvent.UndoClicked)

        assertTrue("unarchiveCommitment(rent)" in repository.calls)
        assertTrue(viewModel.state.value.rows.any { it.id == "rent" })
    }

    @Test
    fun deletingAContributionArchivesBothHalvesThroughTheLink() {
        loaded()
        viewModel.onEvent(CommitmentsEvent.DeleteClicked("gift"))
        viewModel.onEvent(CommitmentsEvent.UndoClicked)

        assertTrue("archiveLinkedCommitment(l1)" in repository.calls)
        assertTrue("unarchiveLinkedCommitment(l1)" in repository.calls)
    }

    @Test
    fun readOnlyRowsCannotBeDeleted() {
        loaded()
        viewModel.onEvent(CommitmentsEvent.DeleteClicked("incoming"))

        assertTrue(repository.calls.none { it.startsWith("archive") })
    }

    @Test
    fun failedArchiveRestoresTheRowAndShowsTheError() {
        repository.archiveResult = Result.failure(dev.lacruzjs.sobreio.domain.error.AppError.Network)
        loaded()
        viewModel.onEvent(CommitmentsEvent.DeleteClicked("rent"))

        assertTrue(viewModel.state.value.rows.any { it.id == "rent" })
        assertEquals(dev.lacruzjs.sobreio.domain.error.AppError.Network, viewModel.state.value.actionError)
    }

    @Test
    fun reopenUnarchivesAndUndoArchivesAgain() {
        loaded()
        viewModel.onEvent(CommitmentsEvent.ReopenClicked("old"))

        assertTrue("unarchiveCommitment(old)" in repository.calls)
        assertTrue(viewModel.state.value.rows.any { it.id == "old" })
        assertEquals(Res.string.commitment_reopened, viewModel.state.value.undoMessage?.message)

        viewModel.onEvent(CommitmentsEvent.UndoClicked)

        assertTrue("archiveCommitment(old)" in repository.calls)
    }

    private fun moving(id: String, name: String, from: String, to: String, on: LocalDate, amountAfter: Long = 9_000): Commitment {
        val first = version(id, CommitmentKind.Expense, account = from).copy(effectiveTo = on)
        val second = version(id, CommitmentKind.Expense, account = to, amount = amountAfter)
            .copy(id = "v2-$id", effectiveFrom = on, effectiveTo = null)
        return Commitment(id, "g1", name, versions = listOf(second, first))
    }

    @Test
    fun aCommitmentThatMovesElsewhereLaterSaysWhereAndWhen() {
        repository.commitments = listOf(moving("gym", "Gym", "acc", "other", LocalDate(2026, 12, 1)))
        loaded()

        val row = viewModel.state.value.rows.first { it.id == "gym" }
        assertEquals("Cash", row.movesToName)
        assertEquals(LocalDate(2026, 12, 1), row.movesOn)
        assertNull(row.startsOn)
        assertEquals(-10_000, row.signedCents)
    }

    @Test
    fun aCommitmentThatMovesHereLaterShowsTheFutureVersion() {
        repository.commitments = listOf(moving("gym", "Gym", "other", "acc", LocalDate(2026, 12, 1)))
        loaded()

        val row = viewModel.state.value.rows.first { it.id == "gym" }
        assertEquals(LocalDate(2026, 12, 1), row.startsOn)
        assertEquals(-9_000, row.signedCents)
        assertEquals("v2-gym", row.version.id)
        assertNull(row.movesToName)
        assertTrue(row.editable)
    }

    @Test
    fun aCommitmentThatAlreadyLeftIsNotListed() {
        repository.commitments = listOf(moving("gym", "Gym", "acc", "other", LocalDate(2026, 10, 1)))
        loaded()

        assertTrue(viewModel.state.value.rows.none { it.id == "gym" })
    }

    @Test
    fun aCommitmentWithARegisteredDueDateInThePeriodIsPaidAndKeepsItsNextDue() {
        repository.paidIds = setOf("rent")
        detail.upcoming = listOf(UpcomingItem("rent", "Rent", LocalDate(2026, 12, 5), -70_000))
        loaded()

        val rows = viewModel.state.value.rows
        assertTrue(rows.first { it.id == "rent" }.paidThisPeriod)
        assertFalse(rows.first { it.id == "salary" }.paidThisPeriod)
        // The screen shows "Paid this period" instead of the date, which the row still carries
        assertEquals(LocalDate(2026, 12, 5), rows.first { it.id == "rent" }.nextDue)
        assertTrue("getPaidCommitmentIds(2026-11-01, 2026-11-30)" in repository.calls)
    }

    @Test
    fun archivedCommitmentsAreNeverAskedAboutPayments() {
        repository.paidIds = setOf("old")
        loaded()

        assertFalse(viewModel.state.value.archivedRows.single().paidThisPeriod)
    }

    @Test
    fun withoutThePaidReadTheRowsAreStillListed() {
        repository.paidResult = Result.failure(dev.lacruzjs.sobreio.domain.error.AppError.Network)
        loaded()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.rows.none { it.paidThisPeriod })
        assertTrue(viewModel.state.value.rows.isNotEmpty())
    }
}
