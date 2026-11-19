package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** A pending due date the movement being typed probably is: registering it settles the due date instead of a duplicate. */
data class PendingSuggestion(
    val commitmentId: String,
    val name: String,
    val kind: CommitmentKind,
    val dueDate: LocalDate,
    val amountCents: Long,
) {
    val key: String get() = "$commitmentId|$dueDate"
}

/**
 * Asks the database which pending due date a new expense or income looks like (`match_pending`) and keeps the first one
 * of the same kind: an expense only suggests an expense commitment and an income an income one (a transfer or a
 * contribution is registered from its own form). The kind comes from `upcoming_occurrences` around the date.
 */
class PendingMatcher(
    private val groupId: String,
    private val commitments: CommitmentRepository,
    private val summary: GroupSummaryRepository,
) {
    suspend fun find(kind: EntryKind, accountId: String, cents: Long, date: LocalDate): PendingSuggestion? {
        val wanted = when (kind) {
            EntryKind.Expense -> CommitmentKind.Expense
            EntryKind.Income -> CommitmentKind.Income
            EntryKind.Transfer -> return null
        }
        val signed = if (kind == EntryKind.Expense) -cents else cents
        val matches = commitments.matchPending(accountId, signed, date).getOrNull().orEmpty()
        if (matches.isEmpty()) return null
        val around = summary.getUpcoming(groupId, date.minus(WINDOW_DAYS, DateTimeUnit.DAY), date.plus(WINDOW_DAYS, DateTimeUnit.DAY))
            .getOrNull().orEmpty()
        return matches.firstNotNullOfOrNull { match ->
            around.firstOrNull { it.commitmentId == match.commitmentId && it.dueDate == match.dueDate && it.kind == wanted }
                ?.let { PendingSuggestion(match.commitmentId, match.name, it.kind, match.dueDate, match.amountCents) }
        }
    }

    private companion object {
        /** The window `match_pending` looks in by default. */
        const val WINDOW_DAYS = 5
    }
}
