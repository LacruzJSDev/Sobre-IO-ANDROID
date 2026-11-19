package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopePurpose
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_accounts_differ
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_destination_account
import dev.lacruzjs.sobreio.generated.resources.validation_destination_group
import dev.lacruzjs.sobreio.generated.resources.validation_end_date_before_start
import dev.lacruzjs.sobreio.generated.resources.validation_envelope_required
import dev.lacruzjs.sobreio.generated.resources.validation_funded_needs_bills
import dev.lacruzjs.sobreio.generated.resources.validation_funded_no_bills
import dev.lacruzjs.sobreio.generated.resources.validation_required
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitmentFormRulesTest {

    private val day = LocalDate(2026, 11, 14)
    private val bank = Account("acc", "g1", "Bank", AccountKind.Bank, true, 100_000)
    private val cash = Account("acc2", "g1", "Cash", AccountKind.Cash, true, 5_000)
    private val vault = Account("out", "g1", "Vault", AccountKind.Savings, false, 9_000)
    private val bills = Envelope("bills", "g1", "Rent", EnvelopeKind.Spending, 0, 0, purpose = EnvelopePurpose.Bills)
    private val free = Envelope("free", "g1", "Fun", EnvelopeKind.Spending, 0, 0, purpose = EnvelopePurpose.Free)

    private val form = CommitmentFormUiState(
        name = "Rent",
        amount = "500",
        accountId = "acc",
        accounts = listOf(bank, cash, vault),
        envelopes = listOf(bills, free),
        envelopeId = "bills",
        firstDueDate = day,
        effectiveFrom = day,
    )

    // Choices ----------------------------------------------------------------------------------------------------------

    @Test
    fun movingToAnAccountOutsideTheEnvelopesDropsEnvelopeAndReserve() {
        val moved = form.copy(funded = true).withAccount(vault)

        assertEquals("out", moved.accountId)
        assertNull(moved.envelopeId)
        assertFalse(moved.funded)
    }

    @Test
    fun aTransferCannotGoToTheAccountItMovesTo() {
        val moved = form.copy(kind = CommitmentKind.Transfer, toAccountId = "acc2").withAccount(cash)

        assertNull(moved.toAccountId)
        assertEquals("bills", moved.envelopeId)
    }

    @Test
    fun turningTheReserveOnKeepsOnlyABillsEnvelope() {
        assertEquals("bills", form.withFunded(true).envelopeId)
        assertNull(form.copy(envelopeId = "free").withFunded(true).envelopeId)
        assertEquals("free", form.copy(envelopeId = "free").withFunded(false).envelopeId)
    }

    @Test
    fun aStoredVersionFillsTheForm() {
        val version = CommitmentVersion(
            id = "v1", commitmentId = "c1", effectiveFrom = day, effectiveTo = null, kind = CommitmentKind.Expense,
            amountCents = 1_250, accountId = "acc", envelopeId = "gone", firstDueDate = day, isRecurring = true,
            everyN = null, unit = null, notes = "Flat",
        )

        val filled = CommitmentFormUiState(accountId = "acc", accounts = listOf(bank), isLoading = true)
            .fromVersion(Commitment("c1", "g1", "Rent"), version, listOf(bills))

        assertFalse(filled.isLoading)
        assertEquals("Rent", filled.name)
        assertEquals("12.50", filled.amount)
        assertNull(filled.envelopeId) // "gone" is no longer a live envelope: it must be picked again
        assertEquals(1, filled.everyN)
        assertEquals(FrequencyUnit.Month, filled.unit)
        assertEquals("Flat", filled.notes)
    }

    // Validation -------------------------------------------------------------------------------------------------------

    @Test
    fun aCompleteExpenseIsValid() {
        assertTrue(validateCommitmentForm(form, "Rent", 50_000, rulesTouched = false).isValid)
    }

    @Test
    fun nameAmountAndEnvelopeAreChecked() {
        val errors = validateCommitmentForm(form.copy(envelopeId = null), "", 0, rulesTouched = false)

        assertEquals(Res.string.validation_required, errors.name)
        assertEquals(Res.string.validation_amount_positive, errors.amount)
        assertEquals(Res.string.validation_envelope_required, errors.envelope)
    }

    @Test
    fun aTransferNeedsAnotherAccount() {
        val transfer = form.copy(kind = CommitmentKind.Transfer)

        assertEquals(Res.string.validation_destination_account, validateCommitmentForm(transfer, "Rent", 100, false).toAccount)
        assertEquals(
            Res.string.validation_accounts_differ,
            validateCommitmentForm(transfer.copy(toAccountId = "acc"), "Rent", 100, false).toAccount,
        )
    }

    @Test
    fun aReserveNeedsABillsEnvelope() {
        assertEquals(
            Res.string.validation_funded_needs_bills,
            validateCommitmentForm(form.copy(funded = true, envelopeId = "free"), "Rent", 100, false).envelope,
        )
        assertEquals(
            Res.string.validation_funded_no_bills,
            validateCommitmentForm(form.copy(funded = true, envelopes = listOf(free), envelopeId = "free"), "Rent", 100, false).envelope,
        )
    }

    @Test
    fun aNameOnlyEditIsNotHeldBackByAnOldReserve() {
        val old = form.copy(isEditing = true, funded = true, envelopeId = "free")

        assertTrue(validateCommitmentForm(old, "Rent", 100, rulesTouched = false).isValid)
        assertEquals(Res.string.validation_funded_needs_bills, validateCommitmentForm(old, "Rent", 100, rulesTouched = true).envelope)
    }

    @Test
    fun aNewContributionNeedsTheGroupThenItsAccount() {
        val contribution = form.copy(kind = CommitmentKind.GroupTransferOut, envelopeId = null)

        assertEquals(Res.string.validation_destination_group, validateCommitmentForm(contribution, "Rent", 100, false).destGroup)
        assertEquals(
            Res.string.validation_destination_account,
            validateCommitmentForm(contribution.copy(destGroupId = "g2"), "Rent", 100, false).destAccount,
        )
        assertTrue(validateCommitmentForm(contribution.copy(destGroupId = "g2", destAccountId = "x"), "Rent", 100, false).isValid)
    }

    @Test
    fun anEditedContributionOnlyChecksNameAndAmount() {
        val locked = form.copy(isEditing = true, isLinkedEdit = true, kind = CommitmentKind.Transfer, envelopeId = null)

        assertTrue(validateCommitmentForm(locked, "Rent", 100, rulesTouched = true).isValid)
    }

    @Test
    fun theEndCannotComeBeforeTheFirstDueDate() {
        val ending = form.copy(endDate = LocalDate(2026, 11, 1))

        assertEquals(Res.string.validation_end_date_before_start, validateCommitmentForm(ending, "Rent", 100, false).endDate)
        assertNull(validateCommitmentForm(ending.copy(isRecurring = false), "Rent", 100, false).endDate)
    }

    @Test
    fun theErrorsShowUnderTheirFields() {
        val shown = form.withErrors(validateCommitmentForm(form.copy(envelopeId = null), "", null, false))

        assertEquals(Res.string.validation_required, shown.nameError)
        assertEquals(Res.string.validation_envelope_required, shown.envelopeError)
        assertNull(shown.endDateError)
    }

    // What Save sends --------------------------------------------------------------------------------------------------

    @Test
    fun aDraftLeavesOutWhatTheKindDoesNotUse() {
        val expense = form.copy(toAccountId = "acc2", endDate = LocalDate(2027, 1, 1), isRecurring = false, notes = "  ")

        val draft = commitmentDraftOf(expense, "Rent", 50_000, day)

        assertNull(draft.toAccountId)
        assertEquals("bills", draft.envelopeId)
        assertNull(draft.endDate)
        assertNull(draft.notes)

        val transfer = commitmentDraftOf(form.copy(kind = CommitmentKind.Transfer, toAccountId = "acc2"), "Save", 100, day)
        assertEquals("acc2", transfer.toAccountId)
        assertNull(transfer.envelopeId)
    }

    @Test
    fun aContributionDraftSendsBothSides() {
        val contribution = form.copy(
            kind = CommitmentKind.GroupTransferOut, destAccountId = "theirs", destEnvelopeId = "their-env", autoRegister = true,
        )

        val draft = linkedCommitmentDraftOf(contribution, "Share", 20_000, day)

        assertEquals("acc", draft.outAccountId)
        assertEquals("theirs", draft.inAccountId)
        assertEquals("bills", draft.outEnvelopeId)
        assertEquals("their-env", draft.inEnvelopeId)
        assertTrue(draft.autoRegister)
    }

    @Test
    fun aChangeCarriesTheApplyFromDay() {
        val from = LocalDate(2026, 12, 1)

        val change = commitmentChangeOf(form.copy(effectiveFrom = from, accountId = "acc2", notes = " New "), 60_000, day)

        assertEquals(from, change.effectiveFrom)
        assertEquals("acc2", change.accountId)
        assertEquals(60_000, change.amountCents)
        assertEquals("New", change.notes)
    }
}
