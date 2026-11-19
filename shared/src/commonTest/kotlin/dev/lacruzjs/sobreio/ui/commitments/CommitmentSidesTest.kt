package dev.lacruzjs.sobreio.ui.commitments

import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** How a commitment looks from one account: its section of the Commitments tab and the sign of its amount. */
class CommitmentSidesTest {

    private val day = LocalDate(2026, 11, 1)

    private fun version(kind: CommitmentKind, accountId: String = "acc", toAccountId: String? = null) = CommitmentVersion(
        id = "v", commitmentId = "c", effectiveFrom = day, effectiveTo = null, kind = kind, amountCents = 5_000,
        accountId = accountId, toAccountId = toAccountId, firstDueDate = day,
    )

    @Test
    fun eachKindHasItsSection() {
        assertEquals(CommitmentSection.Expenses, sectionOf(version(CommitmentKind.Expense), "acc"))
        assertEquals(CommitmentSection.Incomes, sectionOf(version(CommitmentKind.Income), "acc"))
        assertEquals(CommitmentSection.Contributions, sectionOf(version(CommitmentKind.GroupTransferOut), "acc"))
        assertEquals(CommitmentSection.ReceivedContributions, sectionOf(version(CommitmentKind.GroupTransferIn), "acc"))
    }

    @Test
    fun aTransferIsOutgoingFromItsSourceAndIncomingElsewhere() {
        val transfer = version(CommitmentKind.Transfer, accountId = "acc", toAccountId = "savings")

        assertEquals(CommitmentSection.Transfers, sectionOf(transfer, "acc"))
        assertEquals(CommitmentSection.IncomingTransfers, sectionOf(transfer, "savings"))
        assertEquals(-5_000, signedAmount(transfer, "acc"))
        assertEquals(5_000, signedAmount(transfer, "savings"))
    }

    @Test
    fun whatLeavesTheAccountIsNegative() {
        assertEquals(-5_000, signedAmount(version(CommitmentKind.Expense), "acc"))
        assertEquals(-5_000, signedAmount(version(CommitmentKind.GroupTransferOut), "acc"))
        assertEquals(5_000, signedAmount(version(CommitmentKind.Income), "acc"))
        assertEquals(5_000, signedAmount(version(CommitmentKind.GroupTransferIn), "acc"))
    }
}
