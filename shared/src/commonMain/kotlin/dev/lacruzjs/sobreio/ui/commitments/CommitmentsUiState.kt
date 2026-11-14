package dev.lacruzjs.sobreio.ui.commitments

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate

/** The sections of the tab, in the order they are shown. */
enum class CommitmentSection {
    Expenses,
    Incomes,

    /** This account is the source. */
    Transfers,

    /** This account is the destination: read only, the commitment belongs to the source account. */
    IncomingTransfers,

    /** Contributions to another group (`group_transfer_out`). */
    Contributions,

    /** Contributions received from another group (`group_transfer_in`): read only. */
    ReceivedContributions,
}

/**
 * One commitment as this account sees it: [version] is the one in force today (or the newest), or the future one that brings it here, [signedCents] its amount
 * with the sign for this account, [nextDue] the next due date (none when archived or finished). A row is [editable] when
 * the user may edit it from here; a contribution also needs its [link], which is invisible to members of only one group.
 */
data class CommitmentRow(
    val commitment: Commitment,
    val version: CommitmentVersion,
    val section: CommitmentSection,
    val signedCents: Long,
    val nextDue: LocalDate?,
    val link: CommitmentLink?,
    val editable: Boolean,
    /** Set when [version] is a future one that moves the commitment into this account: the day it starts here. */
    val startsOn: LocalDate? = null,
    /** Set (with [movesOn]) when the next version moves the commitment to another account: that account's name. */
    val movesToName: String? = null,
    val movesOn: LocalDate? = null,
) {
    val id: String get() = commitment.id
}

data class AccountCommitmentsUiState(
    /** The live commitments, ordered by name inside each section. */
    val rows: List<CommitmentRow> = emptyList(),
    /** The archived ones: the collapsed "Archived" section at the end. */
    val archivedRows: List<CommitmentRow> = emptyList(),
    /** Set once loaded; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin of a live group: they may create, edit, delete and reopen. Members only read. */
    val canEdit: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Failure of deleting, reopening or undoing, shown above the list. */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
) {
    /** The non-empty sections with their live rows, in display order. */
    val sections: List<Pair<CommitmentSection, List<CommitmentRow>>>
        get() = CommitmentSection.entries.mapNotNull { section ->
            rows.filter { it.section == section }.takeIf { it.isNotEmpty() }?.let { section to it }
        }

    val isEmpty: Boolean get() = rows.isEmpty() && archivedRows.isEmpty()
}

/** The section of [version] as seen from [accountId]. */
internal fun sectionOf(version: CommitmentVersion, accountId: String): CommitmentSection = when (version.kind) {
    CommitmentKind.Expense -> CommitmentSection.Expenses
    CommitmentKind.Income -> CommitmentSection.Incomes
    CommitmentKind.Transfer ->
        if (version.accountId == accountId) CommitmentSection.Transfers else CommitmentSection.IncomingTransfers
    CommitmentKind.GroupTransferOut -> CommitmentSection.Contributions
    CommitmentKind.GroupTransferIn -> CommitmentSection.ReceivedContributions
}

/** The amount with the sign it has for [accountId]: what leaves it is negative. Same rule as `forecast_events`. */
internal fun signedAmount(version: CommitmentVersion, accountId: String): Long = when (version.kind) {
    CommitmentKind.Income, CommitmentKind.GroupTransferIn -> version.amountCents
    CommitmentKind.Transfer -> if (version.accountId == accountId) -version.amountCents else version.amountCents
    CommitmentKind.Expense, CommitmentKind.GroupTransferOut -> -version.amountCents
}
