package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/** How an envelope stands against what its type asks of it, as `envelope_progress` says (D-46). */
enum class ProgressState {
    /** Monthly: [EnvelopeProgress.currentCents] is what is left of [EnvelopeProgress.goalCents] this period. */
    Remaining,

    /** Bills: reserved so far of the cycle amount. */
    Reserving,

    /** Bills: every payment due this period is registered. */
    Paid,

    /** Savings: assigned this period against the monthly contribution. */
    Contributing,

    /** The balance is below zero; [EnvelopeProgress.currentCents] is that balance. */
    Overspent,

    /** Nothing to measure: no bar. */
    None,
}

/** One row of `envelope_progress`. The app sums nothing: the bar only clamps current/goal to 0..1. */
data class EnvelopeProgress(
    val envelopeId: String,
    val purpose: EnvelopePurpose?,
    val state: ProgressState,
    val currentCents: Long,
    val goalCents: Long,
    /** Bills: the earliest unresolved due date. */
    val nextDue: LocalDate? = null,
    /** Bills in [ProgressState.Paid]: the date of the latest payment of the period. */
    val paidOn: LocalDate? = null,
    /** Due dates nobody has registered or skipped yet, pointing at this envelope. */
    val pending: Int = 0,
) {
    /** Fill of the bar, 0..1: a display clamp of the two database figures, not a balance computation. */
    val fraction: Float
        get() = when {
            state == ProgressState.Paid || state == ProgressState.Overspent -> 1f
            goalCents <= 0L -> 0f
            else -> (currentCents.toDouble() / goalCents.toDouble()).toFloat().coerceIn(0f, 1f)
        }

    /** Whether a bar is drawn. */
    val hasBar: Boolean get() = state != ProgressState.None
}
