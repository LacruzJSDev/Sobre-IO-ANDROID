package dev.lacruzjs.sobreio.ui.movementform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.PendingMatch
import dev.lacruzjs.sobreio.fake.FakeGroupSummaryRepository
import dev.lacruzjs.sobreio.fake.FakeCommitmentRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.StoredTransaction
import dev.lacruzjs.sobreio.domain.model.StoredTransfer
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.fake.FakeAccountDetailRepository
import dev.lacruzjs.sobreio.fake.FakeAccountRepository
import dev.lacruzjs.sobreio.fake.FakeAuthRepository
import dev.lacruzjs.sobreio.fake.FakeEnvelopeRepository
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.fake.FakeLastAccountStore
import dev.lacruzjs.sobreio.fake.FakeMovementFormRepository
import dev.lacruzjs.sobreio.fake.FakeTagRepository
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_duplicate_name
import dev.lacruzjs.sobreio.generated.resources.validation_account_required
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.generated.resources.validation_tag_name_too_long
import dev.lacruzjs.sobreio.ui.theme.TagColor
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementFormViewModelTest : MainDispatcherTest() {

    private val today = LocalDate(2026, 11, 14)
    private val bank = Account("acc", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("acc2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val vault = Account("out", "g1", "Vault", AccountKind.Savings, false, 9_000)
    private val unassigned = Envelope("sys", "g1", "Sin asignar", EnvelopeKind.System, 0, 2_000)
    private val food = Envelope("food", "g1", "Food", EnvelopeKind.Spending, 0, 5_000)
    private val fun_ = Envelope("fun", "g1", "Fun", EnvelopeKind.Spending, 0, 3_000)
    private val broke = Envelope("broke", "g1", "Broke", EnvelopeKind.Spending, 0, 0)

    private val accounts = FakeAccountRepository(listOf(bank, cash, vault))
    private val envelopes = FakeEnvelopeRepository(listOf(unassigned, food, fun_, broke))
    private val groups = FakeGroupRepository(
        listOf(
            Group("g1", "Casa", "EUR", GroupRole.Owner),
            Group("g2", "Pareja", "EUR", GroupRole.Member),
            Group("g3", "Dollars", "USD", GroupRole.Owner),
        ),
    )
    private val tags = FakeTagRepository(listOf(Tag("t1", "g1", "Trip"), Tag("t2", "g1", "Home")))
    private val form = FakeMovementFormRepository()
    private val detail = FakeAccountDetailRepository()
    private val auth = FakeAuthRepository(AuthState.SignedIn(User("u1", "ana@example.com", "Ana")))
    private val last = FakeLastAccountStore()
    private val commitments = FakeCommitmentRepository()
    private val summary = FakeGroupSummaryRepository()

    private fun vm(movementId: String? = null, source: String? = null, fixed: String? = null, prefill: MovementPrefill? = null) =
        MovementFormViewModel("g1", movementId, source, fixed, prefill, form, groups, accounts, envelopes, tags, detail, auth, last, commitments, summary)

    private fun MovementFormViewModel.type(amount: String) = onEvent(MovementFormEvent.AmountChanged(amount))

    private fun dueLight(kind: CommitmentKind = CommitmentKind.Expense) =
        DueOccurrence("light", "v1", "Light", kind, LocalDate(2026, 11, 12), 4_500, "acc", null, "food", false)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun aMovementThatLooksLikeAPendingDueDateOffersToRegisterIt() {
        commitments.matches = listOf(PendingMatch("light", "Light", LocalDate(2026, 11, 12), 4_500, 2))
        summary.upcoming = listOf(dueLight())
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.type("47,10")
        testDispatcher.scheduler.advanceTimeBy(500)

        assertEquals("light", vm.state.value.suggestion?.commitmentId)

        vm.onEvent(MovementFormEvent.SuggestionAccepted)

        assertTrue("confirm(light, 2026-11-12)" in summary.calls)
        assertEquals(4_710L, summary.confirmedAmount)
        assertEquals(today, summary.confirmedDate)
        assertTrue(vm.state.value.saved)
        assertTrue(form.calls.none { it.startsWith("createTransaction") })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun anIncomeIsNotOfferedAnExpenseDueDateAndANoIsRemembered() {
        commitments.matches = listOf(PendingMatch("light", "Light", LocalDate(2026, 11, 12), 4_500, 2))
        summary.upcoming = listOf(dueLight())
        val vm = vm()
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Income))
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.type("45")
        testDispatcher.scheduler.advanceTimeBy(500)
        assertNull(vm.state.value.suggestion)

        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Expense))
        testDispatcher.scheduler.advanceTimeBy(500)
        assertEquals("light", vm.state.value.suggestion?.commitmentId)
        vm.onEvent(MovementFormEvent.SuggestionDismissed)
        vm.type("45,5")
        testDispatcher.scheduler.advanceTimeBy(500)
        assertNull(vm.state.value.suggestion)
    }

    @Test
    fun expenseStartsWithNoEnvelopeAndTheGroupsToday() {
        val s = vm().state.value

        assertFalse(s.isLoading)
        assertEquals(EntryKind.Expense, s.kind)
        assertNull(s.envelopeId)
        assertNull(s.accountId)
        assertEquals(today, s.date)
        assertEquals(today, s.today)
        // Only the live group with the same currency
        assertEquals(listOf("g2"), s.groups.map { it.id })
    }

    @Test
    fun incomeStartsWithUnassignedAndSwitchingBackClearsIt() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Income))
        assertEquals("sys", vm.state.value.envelopeId)

        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Expense))
        assertNull(vm.state.value.envelopeId)
    }

    @Test
    fun accountDefaultsToTheFixedOneThenTheLastUsedOne() {
        last.set("u1", "g1", "acc2")

        assertEquals("acc", vm(fixed = "acc").state.value.accountId)
        assertEquals("acc2", vm().state.value.accountId)
        // An account that is no longer live is ignored
        last.set("u1", "g1", "gone")
        assertNull(vm().state.value.accountId)
    }

    @Test
    fun validatesTheRequiredFields() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.SaveClicked)

        val s = vm.state.value
        assertEquals(Res.string.validation_amount_positive, s.amountError)
        assertEquals(Res.string.validation_account_required, s.accountError)
        assertEquals(Res.string.validation_envelope_required, s.envelopeError)
        assertTrue(form.calls.isEmpty())
    }

    @Test
    fun anAccountOutsideTheEnvelopesHidesTheEnvelopeAndDoesNotRequireIt() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("out"))
        assertFalse(vm.state.value.showsEnvelope)

        vm.type("10")
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.createdTransaction!!
        assertEquals("out", draft.accountId)
        assertNull(draft.envelopeId)
        assertEquals(EntryKind.Expense, draft.kind)
        assertEquals(1_000, draft.amountCents)
    }

    @Test
    fun savesAnExpenseAndRemembersTheAccount() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.onEvent(MovementFormEvent.EnvelopeSelected("food"))
        vm.type("12,50")
        vm.onEvent(MovementFormEvent.NotesChanged("  lunch "))
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.createdTransaction!!
        assertEquals(1_250, draft.amountCents)
        assertEquals("food", draft.envelopeId)
        assertEquals("lunch", draft.notes)
        assertEquals(today, draft.date)
        assertTrue(draft.cover.isEmpty())
        assertTrue(vm.state.value.saved)
        assertEquals("acc", last.get("u1", "g1"))
    }

    @Test
    fun incomeSendsUnassignedExplicitly() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Income))
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.type("5")
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.createdTransaction!!
        assertEquals(EntryKind.Income, draft.kind)
        assertEquals("sys", draft.envelopeId)
    }

    @Test
    fun coverNoteShowsOnlyWhenTheEnvelopeHasLessThanTheAmount() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.onEvent(MovementFormEvent.EnvelopeSelected("food"))
        vm.type("40")
        assertFalse(vm.state.value.showsCoverNote)

        vm.type("50.01")
        assertTrue(vm.state.value.showsCoverNote)
        // Candidates have money, are not the chosen one and not Unassigned
        assertEquals(listOf("fun"), vm.state.value.coverCandidates.map { it.id })

        // Unassigned has nothing to cover from
        vm.onEvent(MovementFormEvent.EnvelopeSelected("sys"))
        assertFalse(vm.state.value.showsCoverNote)
    }

    @Test
    fun theChosenCoverEnvelopeTravelsWithItsFullBalance() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.onEvent(MovementFormEvent.EnvelopeSelected("food"))
        vm.type("60")
        vm.onEvent(MovementFormEvent.CoverSelected("fun"))
        vm.onEvent(MovementFormEvent.SaveClicked)

        assertEquals(listOf(CoverItem("fun", 3_000)), form.createdTransaction!!.cover)
    }

    @Test
    fun changingTheEnvelopeDropsTheCoverChoice() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.EnvelopeSelected("food"))
        vm.onEvent(MovementFormEvent.CoverSelected("fun"))
        vm.onEvent(MovementFormEvent.EnvelopeSelected("broke"))

        assertNull(vm.state.value.coverEnvelopeId)
    }

    @Test
    fun aTransferPrefillOpensTheFormFilledAsATransferDatedToday() {
        val s = vm(prefill = MovementPrefill(EntryKind.Transfer, "acc2", "acc", 12_550)).state.value

        assertFalse(s.isLoading)
        assertEquals(EntryKind.Transfer, s.kind)
        assertEquals("acc2", s.accountId)
        assertEquals("acc", s.toAccountId)
        assertEquals("125.50", s.amount)
        assertEquals(today, s.date)
        assertFalse(s.isEditing)
    }

    @Test
    fun aPrefillNeverPicksTheSameAccountTwiceOrAnArchivedOne() {
        val s = vm(prefill = MovementPrefill(EntryKind.Transfer, "acc", "acc", 1_000)).state.value

        assertEquals("acc", s.accountId)
        assertNull(s.toAccountId)
    }

    @Test
    fun aPrefillIsIgnoredWhenEditing() {
        form.transaction = storedExpense()
        val s = vm("tx1", "Transaction", prefill = MovementPrefill(EntryKind.Transfer, "acc2", "acc", 5_000)).state.value

        assertEquals(EntryKind.Expense, s.kind)
        assertNull(s.toAccountId)
    }

    @Test
    fun theRouteFieldsBecomeAPrefillOnlyWithAKnownKind() {
        assertEquals(MovementPrefill(EntryKind.Transfer, "a", "b", 5), MovementPrefill.of("Transfer", "a", "b", 5))
        assertNull(MovementPrefill.of(null, "a", "b", 5))
        assertNull(MovementPrefill.of("Nonsense", "a", "b", 5))
    }

    @Test
    fun transferNeedsDifferentAccounts() {
        val vm = vm(fixed = "acc")
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Transfer))
        vm.type("10")
        vm.onEvent(MovementFormEvent.SaveClicked)
        assertEquals(Res.string.validation_destination_account, vm.state.value.toAccountError)

        // Choosing the source as destination is refused; picking the destination first and the source after clears it
        vm.onEvent(MovementFormEvent.ToAccountSelected("acc"))
        vm.onEvent(MovementFormEvent.SaveClicked)
        assertEquals(Res.string.validation_accounts_differ, vm.state.value.toAccountError)
        assertTrue(form.calls.isEmpty())

        vm.onEvent(MovementFormEvent.ToAccountSelected("acc2"))
        vm.onEvent(MovementFormEvent.SaveClicked)
        val draft = form.createdTransfer!!
        assertEquals("acc", draft.fromAccountId)
        assertEquals("acc2", draft.toAccountId)
        assertEquals(1_000, draft.amountCents)
    }

    @Test
    fun aTransferAcrossTheEnvelopeBoundaryNotesUnassigned() {
        val vm = vm(fixed = "acc")
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Transfer))
        vm.onEvent(MovementFormEvent.ToAccountSelected("acc2"))
        assertFalse(vm.state.value.passesThroughUnassigned)

        vm.onEvent(MovementFormEvent.ToAccountSelected("out"))
        assertTrue(vm.state.value.passesThroughUnassigned)
    }

    @Test
    fun aGroupTransferNeedsAGroupAndAnAccountAndCallsItsFunction() {
        val vm = vm(fixed = "acc")
        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Transfer))
        vm.onEvent(MovementFormEvent.CrossGroupChanged(true))
        vm.type("20")
        vm.onEvent(MovementFormEvent.SaveClicked)
        assertEquals(Res.string.validation_destination_group, vm.state.value.destGroupError)

        accounts.accounts = accounts.accounts + Account("p1", "g2", "Joint", AccountKind.Bank, true, 0)
        vm.onEvent(MovementFormEvent.DestGroupSelected("g2"))
        assertTrue(vm.state.value.destAccounts.any { it.id == "p1" })
        vm.onEvent(MovementFormEvent.DestAccountSelected("p1"))
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.createdGroupTransfer!!
        assertEquals("acc", draft.outAccountId)
        assertEquals("p1", draft.inAccountId)
        assertEquals(2_000, draft.amountCents)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun createsATagFromTheSearchTextAndSelectsIt() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.TagQueryChanged("  Gifts   2026 "))
        assertEquals("Gifts 2026", vm.state.value.tagPicker.createName)
        vm.onEvent(MovementFormEvent.TagCreateClicked)

        val s = vm.state.value
        assertEquals(listOf("Gifts 2026"), tags.created)
        assertEquals(listOf<String?>(TagColor.Default.hex), tags.createdColors)
        assertTrue(s.tags.any { it.name == "Gifts 2026" })
        assertEquals(1, s.selectedTagIds.size)
        assertEquals("", s.tagQuery)
    }

    @Test
    fun aTagWithTheSameKeyIsNotOfferedForCreation() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.TagQueryChanged("  TRÍP "))

        val picker = vm.state.value.tagPicker
        assertNull(picker.createName)
        assertEquals("t1", picker.suggestion?.id)
        assertEquals(listOf("t1"), picker.rows.map { it.id })
    }

    @Test
    fun aDuplicateTagNameShowsTheErrorUnderTheSearchField() {
        tags.createResult = Result.failure(AppError.DuplicateName)
        val vm = vm()
        vm.onEvent(MovementFormEvent.TagQueryChanged("Gifts"))
        vm.onEvent(MovementFormEvent.TagCreateClicked)

        assertEquals(Res.string.error_duplicate_name, vm.state.value.tagError)
        assertEquals("Gifts", vm.state.value.tagQuery)
    }

    @Test
    fun aTooLongTagNameIsRejectedBeforeCallingTheDatabase() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.TagQueryChanged("x".repeat(51)))
        vm.onEvent(MovementFormEvent.TagCreateClicked)

        assertEquals(Res.string.validation_tag_name_too_long, vm.state.value.tagError)
        assertTrue(tags.created.isEmpty())
    }

    @Test
    fun selectedTagsFollowTheListOrder() {
        val vm = vm()
        vm.onEvent(MovementFormEvent.TagToggled("t2"))
        vm.onEvent(MovementFormEvent.TagToggled("t1"))

        assertEquals(listOf("t1", "t2"), vm.state.value.selectedTags.map { it.id })
        vm.onEvent(MovementFormEvent.TagToggled("t1"))
        assertEquals(listOf("t2"), vm.state.value.selectedTags.map { it.id })
    }

    @Test
    fun tagsAreSyncedAfterTheMovementAndAFailureKeepsIt() {
        form.syncResult = Result.failure(AppError.Network)
        val vm = vm()
        vm.onEvent(MovementFormEvent.AccountSelected("acc"))
        vm.onEvent(MovementFormEvent.EnvelopeSelected("food"))
        vm.type("5")
        vm.onEvent(MovementFormEvent.TagToggled("t1"))
        vm.onEvent(MovementFormEvent.SaveClicked)

        assertEquals(setOf("t1"), form.syncedAdded)
        assertEquals(AppError.Network, vm.state.value.submitError)
        assertFalse(vm.state.value.saved)

        // Saving again only retries the tags: the movement is not created twice
        form.syncResult = Result.success(Unit)
        vm.onEvent(MovementFormEvent.SaveClicked)
        assertEquals(1, form.calls.count { it == "createTransaction" })
        assertTrue(vm.state.value.saved)
    }

    // Editing --------------------------------------------------------------------------------------------------------

    private fun storedExpense() = StoredTransaction("tx1", "g1", "acc", "food", EntryKind.Expense, 1_000, LocalDate(2026, 11, 10), "lunch")

    @Test
    fun editLoadsTheValuesAndLocksTheKind() {
        form.transaction = storedExpense()
        form.tagIds = setOf("t1")
        val vm = vm("tx1", "Transaction")
        val s = vm.state.value

        assertTrue(s.isEditing)
        assertEquals(EntryKind.Expense, s.kind)
        assertEquals("10", s.amount)
        assertEquals("acc", s.accountId)
        assertEquals("food", s.envelopeId)
        assertEquals(LocalDate(2026, 11, 10), s.date)
        assertEquals("lunch", s.notes)
        assertEquals(setOf("t1"), s.selectedTagIds)
        // The accounts offered are the ones on the same side of the envelope boundary
        assertEquals(listOf("acc", "acc2"), s.accounts.map { it.id })

        vm.onEvent(MovementFormEvent.KindSelected(EntryKind.Income))
        assertEquals(EntryKind.Expense, vm.state.value.kind)
    }

    @Test
    fun editSendsAnUpdateAndTheTagDifference() {
        form.transaction = storedExpense()
        form.tagIds = setOf("t1")
        val vm = vm("tx1", "Transaction")
        vm.type("15")
        vm.onEvent(MovementFormEvent.TagToggled("t1"))
        vm.onEvent(MovementFormEvent.TagToggled("t2"))
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.updatedTransaction!!
        assertEquals(1_500, draft.amountCents)
        assertEquals("food", draft.envelopeId)
        assertTrue(form.calls.contains("updateTransaction(tx1)"))
        assertFalse(form.calls.contains("createTransaction"))
        assertEquals(setOf("t2"), form.syncedAdded)
        assertEquals(setOf("t1"), form.syncedRemoved)
        assertTrue(vm.state.value.saved)
        // Editing does not change the remembered account
        assertNull(last.get("u1", "g1"))
    }

    @Test
    fun editingToAHigherExpenseWarnsThatItIsNotCovered() {
        form.transaction = storedExpense()
        val vm = vm("tx1", "Transaction")
        assertFalse(vm.state.value.showsEditExtraNote)

        vm.type("11")
        assertTrue(vm.state.value.showsEditExtraNote)
        assertFalse(vm.state.value.showsCoverNote)
    }

    @Test
    fun editsAnIncomeWithUnassignedAsTheExplicitSystemEnvelope() {
        form.transaction = StoredTransaction("tx2", "g1", "acc", "sys", EntryKind.Income, 500, today, null)
        val vm = vm("tx2", "Transaction")
        vm.onEvent(MovementFormEvent.SaveClicked)

        assertEquals("sys", form.updatedTransaction!!.envelopeId)
    }

    @Test
    fun editsATransfer() {
        form.transfer = StoredTransfer("tr1", "g1", "acc", "acc2", 700, today, "move")
        val vm = vm("tr1", "AccountTransfer")
        assertEquals(EntryKind.Transfer, vm.state.value.kind)
        assertEquals("acc2", vm.state.value.toAccountId)

        vm.type("8")
        vm.onEvent(MovementFormEvent.SaveClicked)

        val draft = form.updatedTransfer!!
        assertEquals(800, draft.amountCents)
        assertEquals("acc2", draft.toAccountId)
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun aMissingMovementFailsTheLoad() {
        val vm = vm("tx9", "Transaction")

        assertEquals(AppError.NotFound, vm.state.value.loadError)
    }
}
