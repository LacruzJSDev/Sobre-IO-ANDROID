package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_account_required
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementFormRulesTest {

    private val day = LocalDate(2026, 11, 14)
    private val bank = Account("acc", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("acc2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val vault = Account("out", "g1", "Vault", AccountKind.Savings, false, 9_000)
    private val unassigned = Envelope("sys", "g1", "Sin asignar", EnvelopeKind.System, 0, 2_000)
    private val food = Envelope("food", "g1", "Food", EnvelopeKind.Spending, 0, 1_000)
    private val fun_ = Envelope("fun", "g1", "Fun", EnvelopeKind.Spending, 0, 3_000)

    private val form = MovementFormUiState(
        isLoading = false,
        accounts = listOf(bank, cash, vault),
        envelopes = listOf(unassigned, food, fun_),
        date = day,
    )

    // Validation -------------------------------------------------------------------------------------------------------

    @Test
    fun aCompleteExpenseIsValid() {
        val errors = validateMovementForm(form.copy(accountId = "acc", envelopeId = "food", amount = "12"), 1_200)

        assertTrue(errors.isValid)
    }

    @Test
    fun anExpenseNeedsAnAmountAnAccountAndAnEnvelope() {
        val errors = validateMovementForm(form, null)

        assertFalse(errors.isValid)
        assertEquals(Res.string.validation_amount_invalid, errors.amount)
        assertEquals(Res.string.validation_account_required, errors.account)
        assertEquals(Res.string.validation_envelope_required, errors.envelope)
    }

    @Test
    fun incomeAndAccountsOutsideTheEnvelopesNeedNoEnvelope() {
        assertNull(validateMovementForm(form.copy(kind = EntryKind.Income, accountId = "acc"), 100).envelope)
        assertNull(validateMovementForm(form.copy(accountId = "out"), 100).envelope)
    }

    @Test
    fun aTransferNeedsAnotherAccount() {
        val transfer = form.copy(kind = EntryKind.Transfer, accountId = "acc")

        assertEquals(Res.string.validation_destination_account, validateMovementForm(transfer, 100).toAccount)
        assertEquals(Res.string.validation_accounts_differ, validateMovementForm(transfer.copy(toAccountId = "acc"), 100).toAccount)
        assertTrue(validateMovementForm(transfer.copy(toAccountId = "acc2"), 100).isValid)
    }

    @Test
    fun aTransferToAnotherGroupNeedsTheGroupThenItsAccount() {
        val transfer = form.copy(kind = EntryKind.Transfer, accountId = "acc", crossGroup = true)

        val noGroup = validateMovementForm(transfer, 100)
        assertEquals(Res.string.validation_destination_group, noGroup.destGroup)
        assertNull(noGroup.destAccount)
        assertNull(noGroup.toAccount)
        assertEquals(Res.string.validation_destination_account, validateMovementForm(transfer.copy(destGroupId = "g2"), 100).destAccount)
        assertTrue(validateMovementForm(transfer.copy(destGroupId = "g2", destAccountId = "x"), 100).isValid)
    }

    @Test
    fun theErrorsShowUnderTheirFields() {
        val shown = form.withErrors(validateMovementForm(form, null))

        assertEquals(Res.string.validation_amount_invalid, shown.amountError)
        assertEquals(Res.string.validation_account_required, shown.accountError)
        assertEquals(Res.string.validation_envelope_required, shown.envelopeError)
        assertNull(shown.toAccountError)
    }

    // Draft ------------------------------------------------------------------------------------------------------------

    @Test
    fun anIncomeWithNoEnvelopeGoesToUnassigned() {
        val draft = transactionDraftOf(form.copy(kind = EntryKind.Income, accountId = "acc", notes = "  "), "acc", 500, day)

        assertEquals("sys", draft.envelopeId)
        assertNull(draft.notes)
        assertTrue(draft.cover.isEmpty())
    }

    @Test
    fun anAccountOutsideTheEnvelopesSendsNoEnvelope() {
        val draft = transactionDraftOf(form.copy(accountId = "out", envelopeId = "food"), "out", 500, day)

        assertNull(draft.envelopeId)
    }

    @Test
    fun theCoverIsTheWholeBalanceOfTheChosenEnvelope() {
        // Food holds 10.00 and the expense is 25.00: the cover note shows
        val s = form.copy(accountId = "acc", envelopeId = "food", amount = "25", coverEnvelopeId = "fun", notes = " Lunch ")

        val draft = transactionDraftOf(s, "acc", 2_500, day)

        assertEquals(listOf(CoverItem("fun", 3_000)), draft.cover)
        assertEquals("Lunch", draft.notes)
    }

    @Test
    fun noCoverIsSentWhenTheEnvelopeHasEnough() {
        val s = form.copy(accountId = "acc", envelopeId = "food", amount = "5", coverEnvelopeId = "fun")

        assertTrue(transactionDraftOf(s, "acc", 500, day).cover.isEmpty())
    }

    // Prefill ----------------------------------------------------------------------------------------------------------

    @Test
    fun aTransferPrefillTakesLiveAccountsAndTheAmount() {
        val s = form.withPrefill(MovementPrefill(EntryKind.Transfer, "acc2", "acc", 1_250))

        assertEquals(EntryKind.Transfer, s.kind)
        assertEquals("acc2", s.accountId)
        assertEquals("acc", s.toAccountId)
        assertEquals("12.50", s.amount)
    }

    @Test
    fun aPrefillDropsUnknownAccountsAndADestinationEqualToTheSource() {
        val unknown = form.copy(accountId = "acc").withPrefill(MovementPrefill(EntryKind.Transfer, "gone", "acc2", null))
        assertNull(unknown.accountId) // a named source that is not a choice is not replaced by the preselected one
        assertEquals("acc2", unknown.toAccountId)
        assertEquals("", unknown.amount)

        val same = form.withPrefill(MovementPrefill(EntryKind.Transfer, "acc", "acc", null))
        assertNull(same.toAccountId)
    }

    @Test
    fun anIncomePrefillStartsOnUnassignedAndKeepsThePreselectedAccount() {
        val s = form.copy(accountId = "acc").withPrefill(MovementPrefill(EntryKind.Income, null, "acc2", 300))

        assertEquals("acc", s.accountId)
        assertEquals("sys", s.envelopeId)
        assertNull(s.toAccountId)
    }

    @Test
    fun noPrefillLeavesTheFormAsItIs() {
        assertEquals(form, form.withPrefill(null))
    }

    // Tags -------------------------------------------------------------------------------------------------------------

    @Test
    fun tagSyncSendsOnlyTheDifference() {
        val sync = TagSync(setOf("t1", "t2"))

        assertNull(sync.changesFor(setOf("t2", "t1")))
        assertEquals(TagChanges(added = setOf("t3"), removed = setOf("t1")), sync.changesFor(setOf("t2", "t3")))
    }

    @Test
    fun onceSyncedTheChosenTagsAreTheStoredOnes() {
        val sync = TagSync()
        sync.synced(setOf("t1"))

        assertEquals(setOf("t1"), sync.stored)
        assertNull(sync.changesFor(setOf("t1")))
        assertEquals(TagChanges(emptySet(), setOf("t1")), sync.changesFor(emptySet()))
    }
}
