package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/**
 * Totals of the movements a [MovementFilter] picks, as the database sums them (D-54): what entered, what left (a
 * positive figure), the net and how many rows. In the group ledger an internal transfer is neither in nor out.
 */
data class MovementTotals(val inflowCents: Long, val outflowCents: Long, val netCents: Long, val count: Long)

/** [MovementTotals] of one period of the group (`search_movements_by_period`). */
data class PeriodTotals(val start: LocalDate, val end: LocalDate, val inflowCents: Long, val outflowCents: Long, val count: Long)

/** [MovementTotals] of one envelope (`search_movements_by_envelope`); transactions only. */
data class EnvelopeTotals(val envelopeId: String, val inflowCents: Long, val outflowCents: Long, val count: Long)

/** The report page of a filter: per period, oldest first, and per envelope, most spent first. */
data class MovementReport(val periods: List<PeriodTotals>, val envelopes: List<EnvelopeTotals>)
