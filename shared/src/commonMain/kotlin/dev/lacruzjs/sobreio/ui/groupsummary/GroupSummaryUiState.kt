package dev.lacruzjs.sobreio.ui.groupsummary

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.FreePoint
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import dev.lacruzjs.sobreio.domain.model.GroupWarning
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** What the database says explains the difference between the free money today and at the end of the period. */
enum class ProjectionNoteKind {
    /** `committed_shortfall`: what the envelopes lack for the period's commitments. */
    Commitments,

    /** `pending_out`: contributions to other groups still to be paid from "Unassigned". */
    Contributions,

    /** `pending_income`: income and contributions still to arrive in "Unassigned". */
    Income,
}

data class ProjectionNote(val kind: ProjectionNoteKind, val cents: Long)

/** A bar of "Where this period's money went": an envelope and what it spent in the period (positive). */
data class SpendingBar(val envelope: Envelope, val spentCents: Long)

data class GroupSummaryUiState(
    val currencyCode: String = "EUR",
    /** Any member of a live group may review due dates; archived groups and groups pending deletion are read only. */
    val canRecord: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the content. */
    val isLoading: Boolean = true,
    /** Reading again from the database with the figures on screen: the thin bar under the header. */
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val outlook: GroupOutlook? = null,
    /** The group's today. */
    val today: LocalDate? = null,
    /** `group_free_projection`: the free money day by day, from today to the end of the period. */
    val freeProjection: List<FreePoint> = emptyList(),
    /** Due dates to review, oldest first; the review screen is where they are confirmed or skipped. */
    val pending: List<DueOccurrence> = emptyList(),
    /** Due dates from tomorrow to the end of the period. */
    val upcoming: List<DueOccurrence> = emptyList(),
    val warnings: List<GroupWarning> = emptyList(),
    /** The envelopes that spent most in the period, biggest first, at most [MAX_SPENDING_BARS]. */
    val spending: List<SpendingBar> = emptyList(),
    /** Money in and out of the group in the last periods, oldest first. */
    val flows: List<PeriodFlow> = emptyList(),
    /** Failure of reading again while there is content to show. */
    val actionError: AppError? = null,
) {
    /** What the badge of the Group tab and the review card show. */
    val pendingCount: Int get() = pending.size

    /** The oldest due date waiting for review (the list comes oldest first, but the state does not rely on it). */
    val oldestPendingDate: LocalDate? get() = pending.minOfOrNull { it.dueDate }

    /** Days from today to the end of the period (0 on its last day): a display count; null until both are known. */
    val daysLeft: Int?
        get() {
            val end = outlook?.periodEnd ?: return null
            val day = today ?: return null
            return day.daysUntil(end).coerceAtLeast(0)
        }

    /**
     * The lowest day of the projection when it goes below zero after today: choosing the smallest of the database's rows is
     * a selection, not a computation. Null when the projection never goes below zero or is already at its lowest today.
     */
    val lowestPoint: FreePoint?
        get() {
            val first = freeProjection.firstOrNull() ?: return null
            val lowest = freeProjection.minByOrNull { it.freeCents } ?: return null
            return lowest.takeIf { it.freeCents < 0 && it.day > first.day }
        }

    /**
     * The database figure that explains the difference between today and the end of the period, chosen by importance:
     * the commitments the envelopes cannot cover, then the contributions to pay, then the income to arrive. Null when none
     * of them has anything. Nothing is subtracted here.
     */
    val projectionNote: ProjectionNote?
        get() {
            val outlook = outlook ?: return null
            return when {
                outlook.committedShortfallCents > 0 -> ProjectionNote(ProjectionNoteKind.Commitments, outlook.committedShortfallCents)
                outlook.pendingOutCents > 0 -> ProjectionNote(ProjectionNoteKind.Contributions, outlook.pendingOutCents)
                outlook.pendingIncomeCents > 0 -> ProjectionNote(ProjectionNoteKind.Income, outlook.pendingIncomeCents)
                else -> null
            }
        }

    /** Length of a spending bar relative to the biggest one, 0..1: only a display ratio. */
    fun spendingFraction(bar: SpendingBar): Float {
        val biggest = spending.maxOfOrNull { it.spentCents }?.takeIf { it > 0 } ?: return 0f
        return (bar.spentCents.toFloat() / biggest).coerceIn(0f, 1f)
    }

    companion object {
        const val MAX_SPENDING_BARS = 6
    }
}
