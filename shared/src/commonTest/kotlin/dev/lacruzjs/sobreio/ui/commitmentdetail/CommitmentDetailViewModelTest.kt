package dev.lacruzjs.sobreio.ui.commitmentdetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentOccurrence
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.OccurrenceStatus
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeCommitmentRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitmentDetailViewModelTest : MainDispatcherTest() {

    private fun version(
        id: String,
        from: LocalDate,
        to: LocalDate? = null,
        kind: CommitmentKind = CommitmentKind.Expense,
        account: String = "acc",
        toAccount: String? = null,
        amount: Long = 70_000,
        envelope: String? = "food",
    ) = CommitmentVersion(
        id = id, commitmentId = "c1", effectiveFrom = from, effectiveTo = to, kind = kind, amountCents = amount,
        accountId = account, toAccountId = toAccount, envelopeId = envelope, firstDueDate = from, isRecurring = true,
        everyN = 1, unit = FrequencyUnit.Month, anchorDay = 5,
    )

    private val oldVersion = version("v1", LocalDate(2026, 1, 1), LocalDate(2026, 6, 1), account = "other", amount = 60_000, envelope = null)
    private val newVersion = version("v2", LocalDate(2026, 6, 1))
    private val rent = Commitment("c1", "g1", "Rent", versions = listOf(oldVersion, newVersion))

    private val repository = FakeCommitmentRepository(listOf(rent))
    private val groups = FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Owner)))
    private val detail = FakeAccountDetailRepository()
    private val accounts = FakeAccountRepository(
        listOf(Account("acc", "g1", "Bank", AccountKind.Bank, true, 0), Account("other", "g1", "Cash", AccountKind.Cash, true, 0)),
    )
    private val envelopes = FakeEnvelopeRepository(
        listOf(
            Envelope("e0", "g1", "Sin asignar", EnvelopeKind.System, 0, 0),
            Envelope("food", "g1", "Food", EnvelopeKind.Spending, 0, 0),
        ),
    )

    private fun viewModel(commitment: String = "c1", account: String = "acc") =
        CommitmentDetailViewModel("g1", account, commitment, repository, groups, detail, accounts, envelopes)
            .also { it.onEvent(CommitmentDetailEvent.Refresh) }

    @Test
    fun loadsTheDetailOfTheVersionInForce() {
        val state = viewModel().state.value

        assertFalse(state.isLoading)
        assertEquals("Rent", state.name)
        assertEquals("Bank", state.accountName)
        assertEquals("v2", state.version?.id)
        assertEquals(-70_000, state.signedCents)
        assertEquals("EUR", state.currencyCode)
        assertFalse(state.isArchived)
    }

    @Test
    fun occurrencesAreNewestFirstWithTheSignForTheAccount() {
        repository.occurrences = listOf(
            CommitmentOccurrence(LocalDate(2026, 9, 5), OccurrenceStatus.Materialized, -70_000, LocalDate(2026, 9, 5)),
            CommitmentOccurrence(LocalDate(2026, 11, 5), OccurrenceStatus.Materialized, -71_000, LocalDate(2026, 11, 7)),
            CommitmentOccurrence(LocalDate(2026, 10, 5), OccurrenceStatus.Skipped),
        )
        val rows = viewModel().state.value.occurrences

        assertEquals(listOf(LocalDate(2026, 11, 5), LocalDate(2026, 10, 5), LocalDate(2026, 9, 5)), rows.map { it.dueDate })
        assertEquals(-71_000, rows[0].signedCents)
        // The actual date shows only when it differs from the due date
        assertEquals(LocalDate(2026, 11, 7), rows[0].date)
        assertNull(rows[2].date)
    }

    @Test
    fun aSkippedOccurrenceHasNoAmount() {
        repository.occurrences = listOf(CommitmentOccurrence(LocalDate(2026, 10, 5), OccurrenceStatus.Skipped))
        val row = viewModel().state.value.occurrences.single()

        assertTrue(row.skipped)
        assertNull(row.signedCents)
        assertNull(row.date)
    }

    @Test
    fun aTransferOccurrenceIsNegativeInItsSourceAndPositiveInItsDestination() {
        val transfer = Commitment(
            "c1", "g1", "Saving",
            versions = listOf(version("t", LocalDate(2026, 1, 1), kind = CommitmentKind.Transfer, toAccount = "other", amount = 5_000)),
        )
        repository.commitments = listOf(transfer)
        repository.occurrences = listOf(
            CommitmentOccurrence(LocalDate(2026, 11, 5), OccurrenceStatus.Materialized, 5_000, LocalDate(2026, 11, 5), fromAccountId = "acc"),
        )

        assertEquals(-5_000, viewModel(account = "acc").state.value.occurrences.single().signedCents)
        assertEquals(5_000, viewModel(account = "other").state.value.occurrences.single().signedCents)
    }

    @Test
    fun versionsAreNewestFirstAndAClosedOneEndsTheDayBefore() {
        val rows = viewModel().state.value.versions

        assertEquals(listOf("v2", "v1"), rows.map { it.version.id })
        assertNull(rows[0].lastDay)
        // effective_to is exclusive: June 1st means it ran until May 31st
        assertEquals(LocalDate(2026, 5, 31), rows[1].lastDay)
    }

    @Test
    fun aVersionNamesItsAccountOnlyWhenItDiffersFromThePreviousOne() {
        val rows = viewModel().state.value.versions

        assertEquals("Bank", rows[0].accountName)
        // The oldest has no previous version
        assertNull(rows[1].accountName)
    }

    @Test
    fun aVersionNamesItsEnvelopeOrSaysUnassigned() {
        val withUnassigned = rent.copy(versions = listOf(newVersion.copy(envelopeId = "e0")))
        repository.commitments = listOf(withUnassigned)
        val unassigned = viewModel().state.value.versions.single()
        assertTrue(unassigned.unassigned)
        assertNull(unassigned.envelopeName)

        repository.commitments = listOf(rent)
        assertEquals("Food", viewModel().state.value.versions[0].envelopeName)
    }

    @Test
    fun theOwnerSeesThePencilOfALiveEditableCommitment() {
        assertTrue(viewModel().state.value.canEdit)
    }

    @Test
    fun adminsSeeThePencilAndMembersDoNot() {
        groups.groups = listOf(Group("g1", "Casa", "EUR", GroupRole.Admin))
        assertTrue(viewModel().state.value.canEdit)

        groups.groups = listOf(Group("g1", "Casa", "EUR", GroupRole.Member))
        assertFalse(viewModel().state.value.canEdit)
    }

    @Test
    fun anArchivedCommitmentHasNoPencilAndSaysSo() {
        repository.commitments = listOf(rent.copy(archivedAt = "2026-11-01T10:00:00Z"))
        val state = viewModel().state.value

        assertTrue(state.isArchived)
        assertFalse(state.canEdit)
    }

    @Test
    fun readOnlyRowsHaveNoPencil() {
        val incoming = Commitment(
            "c1", "g1", "From other",
            versions = listOf(version("t", LocalDate(2026, 1, 1), kind = CommitmentKind.Transfer, account = "other", toAccount = "acc")),
        )
        repository.commitments = listOf(incoming)
        assertFalse(viewModel().state.value.canEdit)

        val received = Commitment("c1", "g1", "Received", versions = listOf(version("r", LocalDate(2026, 1, 1), kind = CommitmentKind.GroupTransferIn)))
        repository.commitments = listOf(received)
        assertFalse(viewModel().state.value.canEdit)
    }

    @Test
    fun aContributionNeedsItsVisibleLinkForThePencil() {
        val gift = Commitment("c1", "g1", "Gift", versions = listOf(version("g", LocalDate(2026, 1, 1), kind = CommitmentKind.GroupTransferOut)))
        repository.commitments = listOf(gift)
        assertFalse(viewModel().state.value.canEdit)

        repository.links = listOf(CommitmentLink("l1", "g1", "g2", "c1", "c1-in"))
        assertTrue(viewModel().state.value.canEdit)
    }

    @Test
    fun anArchivedGroupHasNoPencil() {
        groups.groups = listOf(Group("g1", "Casa", "EUR", GroupRole.Owner, archivedAt = kotlin.time.Instant.parse("2026-11-01T10:00:00Z")))
        assertFalse(viewModel().state.value.canEdit)
    }

    @Test
    fun aMissingCommitmentIsAnError() {
        val state = viewModel(commitment = "nope").state.value

        assertFalse(state.isLoading)
        assertEquals(AppError.NotFound, state.error)
    }

    @Test
    fun aFailedOccurrencesReadIsAnError() {
        repository.occurrencesResult = Result.failure(AppError.Network)
        assertEquals(AppError.Network, viewModel().state.value.error)
    }
}
