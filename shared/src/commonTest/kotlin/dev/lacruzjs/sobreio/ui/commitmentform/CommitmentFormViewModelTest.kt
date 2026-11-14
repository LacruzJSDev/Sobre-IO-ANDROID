package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeCommitmentRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_end_date_before_start
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitmentFormViewModelTest : MainDispatcherTest() {

    private val today = LocalDate(2026, 11, 14)
    private val bank = Account("acc", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("acc2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val food = Envelope("env", "g1", "Food", EnvelopeKind.Spending, 0, 0)
    private val unassigned = Envelope("sys", "g1", "Sin asignar", EnvelopeKind.System, 0, 0)

    private val accounts = FakeAccountRepository(listOf(bank, cash))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food))
    private val groups = FakeGroupRepository(
        listOf(Group("g1", "Casa", "EUR", GroupRole.Owner), Group("g2", "Pareja", "EUR", GroupRole.Admin), Group("g3", "Dollars", "USD", GroupRole.Owner)),
    )
    private val commitments = FakeCommitmentRepository()
    private val detail = FakeAccountDetailRepository()

    private fun form(commitmentId: String? = null) =
        CommitmentFormViewModel("g1", "acc", commitmentId, commitments, groups, accounts, envelopes, detail)

    private fun version(kind: CommitmentKind = CommitmentKind.Expense, funded: Boolean = false) = CommitmentVersion(
        id = "v1", commitmentId = "c1", effectiveFrom = LocalDate(2026, 1, 1), effectiveTo = null, kind = kind,
        amountCents = 70_050, accountId = "acc", envelopeId = "env".takeIf { kind == CommitmentKind.Expense },
        firstDueDate = LocalDate(2026, 1, 5), isRecurring = true, everyN = 1, unit = FrequencyUnit.Month,
        anchorDay = 5, funded = funded, autoRegister = false, notes = "n",
    )

    private fun stored(kind: CommitmentKind = CommitmentKind.Expense) =
        Commitment("c1", "g1", "Rent", versions = listOf(version(kind)))

    @Test
    fun loadsChoicesAndDefaultsTheDateToToday() {
        val vm = form()
        val state = vm.state.value

        assertFalse(state.isLoading)
        assertEquals(today, state.firstDueDate)
        assertEquals(listOf("Bank", "Cash"), state.accounts.map { it.name })
        assertEquals(listOf("Cash"), state.destinationAccounts.map { it.name })
        assertEquals(listOf("Food"), state.envelopes.map { it.name })
        // Only the live group with the same currency where the user can edit
        assertEquals(listOf("g2"), state.groups.map { it.id })
    }

    @Test
    fun validatesTheFieldsOfAnExpense() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        var state = vm.state.value
        assertEquals(Res.string.validation_required, state.nameError)
        assertEquals(Res.string.validation_amount_positive, state.amountError)
        assertEquals(Res.string.validation_envelope_required, state.envelopeError)
        assertTrue(commitments.calls.isEmpty())

        vm.onEvent(CommitmentFormEvent.EndDateChanged(LocalDate(2026, 1, 1)))
        vm.onEvent(CommitmentFormEvent.SaveClicked)
        state = vm.state.value
        assertEquals(Res.string.validation_end_date_before_start, state.endDateError)
    }

    @Test
    fun transferNeedsADestinationAccount() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.KindSelected(CommitmentKind.Transfer))
        vm.onEvent(CommitmentFormEvent.NameChanged("Saving"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("50"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(Res.string.validation_destination_account, vm.state.value.toAccountError)

        vm.onEvent(CommitmentFormEvent.ToAccountSelected("acc2"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(CommitmentKind.Transfer, commitments.createdDraft?.kind)
        assertEquals("acc2", commitments.createdDraft?.toAccountId)
        assertNull(commitments.createdDraft?.envelopeId)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun createsAnExpenseWithEveryField() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.NameChanged("  Rent "))
        vm.onEvent(CommitmentFormEvent.AmountChanged("700,5"))
        vm.onEvent(CommitmentFormEvent.EnvelopeSelected("env"))
        vm.onEvent(CommitmentFormEvent.AutoRegisterChanged(true))
        vm.onEvent(CommitmentFormEvent.FundedChanged(true))
        vm.onEvent(CommitmentFormEvent.NotesChanged(" flat "))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        val draft = commitments.createdDraft
        assertEquals("Rent", draft?.name)
        assertEquals(70_050L, draft?.amountCents)
        assertEquals("env", draft?.envelopeId)
        assertEquals(true, draft?.funded)
        assertEquals(true, draft?.autoRegister)
        assertEquals("flat", draft?.notes)
        assertEquals(today, draft?.firstDueDate)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun expenseOfAnAccountOutsideTheEnvelopesSendsNoEnvelope() {
        accounts.accounts = listOf(bank.copy(inEnvelopes = false), cash)
        val vm = form()
        vm.onEvent(CommitmentFormEvent.NameChanged("Fee"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("3"))
        vm.onEvent(CommitmentFormEvent.EnvelopeSelected("env"))
        vm.onEvent(CommitmentFormEvent.FundedChanged(true))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertNull(commitments.createdDraft?.envelopeId)
        assertFalse(commitments.createdDraft?.funded ?: true)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun fundedIsOnlyAllowedWithAnEnvelopeAndAMonthlyYearlyOrOneOffSchedule() {
        val vm = form()
        assertFalse(vm.state.value.canFund)
        vm.onEvent(CommitmentFormEvent.EnvelopeSelected("env"))
        assertTrue(vm.state.value.canFund)
        vm.onEvent(CommitmentFormEvent.UnitSelected(FrequencyUnit.Week))
        assertFalse(vm.state.value.canFund)
        vm.onEvent(CommitmentFormEvent.RecurringChanged(false))
        assertTrue(vm.state.value.canFund)
        vm.onEvent(CommitmentFormEvent.KindSelected(CommitmentKind.Income))
        assertFalse(vm.state.value.canFund)
    }

    @Test
    fun oneOffCommitmentSendsNoEndDate() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.KindSelected(CommitmentKind.Income))
        vm.onEvent(CommitmentFormEvent.NameChanged("Bonus"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("100"))
        vm.onEvent(CommitmentFormEvent.EndDateChanged(LocalDate(2027, 1, 1)))
        vm.onEvent(CommitmentFormEvent.RecurringChanged(false))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertNull(commitments.createdDraft?.endDate)
        assertEquals(false, commitments.createdDraft?.isRecurring)
    }

    @Test
    fun contributionNeedsAGroupAndAnAccountAndCreatesTheLinkedPair() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.KindSelected(CommitmentKind.GroupTransferOut))
        // Contributions register themselves by default
        assertTrue(vm.state.value.autoRegister)
        vm.onEvent(CommitmentFormEvent.NameChanged("Rent share"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("300"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)
        assertEquals(Res.string.validation_destination_group, vm.state.value.destGroupError)

        vm.onEvent(CommitmentFormEvent.DestGroupSelected("g2"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)
        assertEquals(Res.string.validation_destination_account, vm.state.value.destAccountError)

        vm.onEvent(CommitmentFormEvent.DestAccountSelected("acc2"))
        vm.onEvent(CommitmentFormEvent.EnvelopeSelected("env"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        val draft = commitments.createdLinkedDraft
        assertEquals("acc", draft?.outAccountId)
        assertEquals("acc2", draft?.inAccountId)
        assertEquals("env", draft?.outEnvelopeId)
        assertNull(draft?.inEnvelopeId)
        assertEquals(30_000L, draft?.amountCents)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun editLoadsTheVersionInForceAndTheHistory() {
        commitments.commitments = listOf(stored())
        val state = form("c1").state.value

        assertEquals("Rent", state.name)
        assertEquals("700.50", state.amount)
        assertEquals("env", state.envelopeId)
        assertEquals(today, state.effectiveFrom)
        assertEquals(1, state.history.size)
        assertTrue(state.isEditing)
    }

    @Test
    fun editChangesFromTheChosenDate() {
        commitments.commitments = listOf(stored())
        val vm = form("c1")
        vm.onEvent(CommitmentFormEvent.AmountChanged("800"))
        vm.onEvent(CommitmentFormEvent.EffectiveFromChanged(LocalDate(2026, 12, 1)))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(listOf("getCommitment(c1)", "changeCommitment(c1)"), commitments.calls)
        assertEquals(80_000L, commitments.changedWith?.amountCents)
        assertEquals(LocalDate(2026, 12, 1), commitments.changedWith?.effectiveFrom)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun renameOnlyDoesNotCreateAVersion() {
        commitments.commitments = listOf(stored())
        val vm = form("c1")
        vm.onEvent(CommitmentFormEvent.NameChanged("Flat rent"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertTrue("renameCommitment(c1, Flat rent)" in commitments.calls)
        assertTrue(commitments.calls.none { it.startsWith("changeCommitment") })
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun retryAfterAFailedChangeDoesNotRenameAgain() {
        commitments.commitments = listOf(stored())
        commitments.changeResult = Result.failure(AppError.BusinessRule("FZ018"))
        val vm = form("c1")
        vm.onEvent(CommitmentFormEvent.NameChanged("Flat rent"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("900"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(AppError.BusinessRule("FZ018"), vm.state.value.submitError)
        assertFalse(vm.state.value.saved)

        commitments.changeResult = Result.success(Unit)
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(1, commitments.calls.count { it.startsWith("renameCommitment") })
        assertEquals(2, commitments.calls.count { it.startsWith("changeCommitment") })
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun editingAContributionChangesOnlyTheAmountThroughTheLink() {
        val gift = Commitment("c1", "g1", "Gift", versions = listOf(version(CommitmentKind.GroupTransferOut)))
        val half = Commitment("c2", "g2", "Gift", versions = listOf(version(CommitmentKind.GroupTransferIn).copy(commitmentId = "c2", accountId = "acc2")))
        commitments.commitments = listOf(gift, half)
        commitments.links = listOf(CommitmentLink("l1", "g1", "g2", "c1", "c2"))
        val vm = form("c1")
        assertTrue(vm.state.value.isLinkedEdit)
        assertEquals("g2", vm.state.value.destGroupId)

        vm.onEvent(CommitmentFormEvent.AmountChanged("10"))
        vm.onEvent(CommitmentFormEvent.EffectiveFromChanged(LocalDate(2026, 12, 1)))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertTrue("changeLinkedCommitment(l1, 1000)" in commitments.calls)
        assertEquals(LocalDate(2026, 12, 1), commitments.linkedFrom)
        assertTrue(commitments.calls.none { it.startsWith("changeCommitment") })
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun takenNameShowsUnderTheNameField() {
        commitments.createResult = Result.failure(AppError.DuplicateName)
        val vm = form()
        vm.onEvent(CommitmentFormEvent.NameChanged("Rent"))
        vm.onEvent(CommitmentFormEvent.AmountChanged("5"))
        vm.onEvent(CommitmentFormEvent.EnvelopeSelected("env"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertTrue(vm.state.value.nameError != null)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun editCanMoveTheCommitmentToAnotherAccountFromTheChosenDate() {
        commitments.commitments = listOf(stored())
        val vm = form("c1")
        assertFalse(vm.state.value.accountMoved)
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))
        vm.onEvent(CommitmentFormEvent.EffectiveFromChanged(LocalDate(2026, 12, 1)))
        assertTrue(vm.state.value.accountMoved)
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals("acc2", commitments.changedWith?.accountId)
        assertEquals("env", commitments.changedWith?.envelopeId)
        assertEquals(LocalDate(2026, 12, 1), commitments.changedWith?.effectiveFrom)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun movingToAnAccountOutsideTheEnvelopesClearsEnvelopeAndFundingAndSkipsItsValidation() {
        accounts.accounts = listOf(bank, cash.copy(inEnvelopes = false))
        commitments.commitments = listOf(Commitment("c1", "g1", "Rent", versions = listOf(version(funded = true))))
        val vm = form("c1")
        assertTrue(vm.state.value.fundedEffective)

        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))
        assertNull(vm.state.value.envelopeId)
        assertFalse(vm.state.value.funded)
        assertFalse(vm.state.value.showsExpenseEnvelope)

        vm.onEvent(CommitmentFormEvent.SaveClicked)
        assertNull(vm.state.value.envelopeError)
        assertNull(commitments.changedWith?.envelopeId)
        assertFalse(commitments.changedWith?.funded ?: true)
        assertEquals("acc2", commitments.changedWith?.accountId)
    }

    @Test
    fun movingBackInsideTheEnvelopesNeedsAnEnvelopeAgain() {
        accounts.accounts = listOf(bank, cash.copy(inEnvelopes = false))
        commitments.commitments = listOf(stored())
        val vm = form("c1")
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)

        assertEquals(Res.string.validation_envelope_required, vm.state.value.envelopeError)
        assertTrue(commitments.calls.none { it.startsWith("changeCommitment") })
    }

    @Test
    fun aTransferCannotGoToItsOwnSource() {
        commitments.commitments = listOf(
            Commitment("c1", "g1", "Saving", versions = listOf(version(CommitmentKind.Transfer).copy(toAccountId = "acc2"))),
        )
        val vm = form("c1")
        assertEquals(listOf("acc2"), vm.state.value.destinationAccounts.map { it.id })

        vm.onEvent(CommitmentFormEvent.ToAccountSelected("acc"))
        vm.onEvent(CommitmentFormEvent.SaveClicked)
        assertEquals(Res.string.validation_accounts_differ, vm.state.value.toAccountError)
        assertTrue(commitments.calls.none { it.startsWith("changeCommitment") })

        // Moving the source onto the destination clears the destination
        vm.onEvent(CommitmentFormEvent.ToAccountSelected("acc2"))
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))
        assertNull(vm.state.value.toAccountId)
    }

    @Test
    fun aLinkedContributionKeepsItsAccountReadOnly() {
        val gift = Commitment("c1", "g1", "Gift", versions = listOf(version(CommitmentKind.GroupTransferOut)))
        val half = Commitment("c2", "g2", "Gift", versions = listOf(version(CommitmentKind.GroupTransferIn).copy(commitmentId = "c2", accountId = "acc2")))
        commitments.commitments = listOf(gift, half)
        commitments.links = listOf(CommitmentLink("l1", "g1", "g2", "c1", "c2"))
        val vm = form("c1")
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))

        assertFalse(vm.state.value.canChangeAccount)
        assertEquals("acc", vm.state.value.accountId)
        assertFalse(vm.state.value.accountMoved)
    }

    @Test
    fun creatingKeepsTheAccountOfTheDetail() {
        val vm = form()
        vm.onEvent(CommitmentFormEvent.AccountSelected("acc2"))

        assertEquals("acc", vm.state.value.accountId)
    }

    @Test
    fun editingAFutureVersionThatMovesHereLoadsThatVersion() {
        val now = version().copy(accountId = "acc2", effectiveTo = LocalDate(2026, 12, 1))
        val next = version().copy(id = "v2", effectiveFrom = LocalDate(2026, 12, 1), amountCents = 90_000)
        commitments.commitments = listOf(Commitment("c1", "g1", "Rent", versions = listOf(next, now)))
        val state = form("c1").state.value

        assertEquals("acc", state.accountId)
        assertEquals("900", state.amount)
        assertEquals(LocalDate(2026, 12, 1), state.effectiveFrom)
    }
}
