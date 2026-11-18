package dev.lacruzjs.sobreio.ui.envelopedetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate

/** A commitment that reserves money for the envelope in the current cycle (`commitment_funding_status`). */
data class FundingRow(
    val commitmentId: String,
    /** The account the commitment is opened from. */
    val accountId: String,
    val name: String,
    val kind: CommitmentKind,
    /** What it sets aside this period, as the database says. */
    val thisPeriodCents: Long,
    val nextDueDate: LocalDate,
)

/**
 * A move between envelopes as this envelope sees it: [signedCents] is positive when the money came in and negative when
 * it went out. [otherName] is the envelope on the other side (null when it is not known) and [otherIsUnassigned] says it
 * is the system one, whose stored name is not shown (the UI has its own, translated).
 */
data class TransferRow(
    val transfer: EnvelopeTransfer,
    val signedCents: Long,
    val otherName: String?,
    val otherIsUnassigned: Boolean,
    /** Day it was entered, in the group's time zone. */
    val date: LocalDate,
) {
    /** Manual moves, monthly targets and reserves can be voided ([TransferOrigin.canVoid]). */
    val canDelete: Boolean get() = transfer.origin.canVoid

    /** Manual moves and monthly targets can be edited ([TransferOrigin.canEdit]). */
    val canEdit: Boolean get() = transfer.origin.canEdit
}

data class EnvelopeDetailUiState(
    /** True until the first load ends; later refreshes keep showing the content. */
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val groupName: String = "",
    /** Set once loaded; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin of a live group. */
    val canEdit: Boolean = false,
    /** Any member of a live group may edit and void moves between envelopes. */
    val canWriteTransfers: Boolean = false,
    val envelope: Envelope? = null,
    /** Name of the holder; null for a common envelope. Only shown when [hasSeveralMembers]. */
    val holderName: String? = null,
    val hasSeveralMembers: Boolean = false,
    /** The row of the current period in `envelope_periods`; null when none has been created yet. */
    val period: EnvelopePeriod? = null,
    /** The last periods, oldest first, for the chart. */
    val chartPeriods: List<EnvelopePeriod> = emptyList(),
    /** Null for the system envelope and archived ones, which the outlook skips. */
    val outlook: EnvelopeOutlook? = null,
    val funding: List<FundingRow> = emptyList(),
    /** The live moves loaded so far, newest first, minus the one hidden while its delete waits for the snackbar. */
    val transfers: List<TransferRow> = emptyList(),
    /** How many live moves the envelope has in all (the hidden one does not count). */
    val transfersTotal: Int = 0,
    val hasMoreTransfers: Boolean = false,
    val isLoadingMoreTransfers: Boolean = false,
    val loadMoreTransfersFailed: Boolean = false,
    /** The newest transactions of the envelope. */
    val latestMovements: List<Movement> = emptyList(),
    /** Names of the group's accounts (archived too), to say which one a movement belongs to. */
    val accountNames: Map<String, String> = emptyMap(),
    /** The group has several live accounts: with one, every row would repeat its name, so the second line leaves it out. */
    val hasSeveralAccounts: Boolean = false,
    /** Failure of a delete or an undo, shown above the content. */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
) {
    /** The system "Unassigned" envelope: read only, with no commitments and a name from the resources. */
    val isUnassigned: Boolean get() = envelope?.kind == EnvelopeKind.System

    /** The pencil: owner or admin of a live group, never for "Unassigned" or an archived envelope. */
    val canEditEnvelope: Boolean get() = canEdit && envelope != null && !isUnassigned && !envelope.isArchived
}

/**
 * The two columns of the period chart: what was assigned and what was spent. A column cannot be negative, so a net
 * assignment below zero and income count as 0; this is a display choice, not a balance.
 */
fun EnvelopePeriod.chartColumns(): List<Long> = listOf(maxOf(0L, assignedCents), maxOf(0L, -activityCents))
