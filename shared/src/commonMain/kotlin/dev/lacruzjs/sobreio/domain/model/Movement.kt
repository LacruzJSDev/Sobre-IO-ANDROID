package dev.lacruzjs.sobreio.domain.model

import kotlinx.datetime.LocalDate

/** Where a movement row comes from: a transaction, or an account transfer (a transfer is voided in its own table). */
enum class MovementSource { Transaction, AccountTransfer }

/**
 * One row of the Movements list (`search_movements`). [amountCents] is signed as the database sends it: for an account's
 * list it is the account's side; in the group ledger a transfer comes once, as the source's `TransferOut` row (account =
 * source, [counterAccountId] = destination, amount negative), and the screen shows it unsigned. [envelopeId] is null for
 * transfers and for contributions seen without envelope. [createdAt] is the timestamp text, only used to keep the
 * database's order when a hidden row comes back.
 */
data class Movement(
    val id: String,
    val source: MovementSource,
    val accountId: String,
    val counterAccountId: String?,
    val envelopeId: String?,
    val kind: MovementKind,
    val amountCents: Long,
    val date: LocalDate,
    val notes: String?,
    val createdBy: String?,
    val createdAt: String,
    val tagIds: List<String> = emptyList(),
) {
    val isAccountTransfer: Boolean get() = kind == MovementKind.TransferOut || kind == MovementKind.TransferIn
    val isGroupTransfer: Boolean get() = kind == MovementKind.GroupTransferOut || kind == MovementKind.GroupTransferIn

    /**
     * Every row can be deleted (voided); a contribution leg voids both legs at once (`void_group_transfer`), and the
     * database decides who may (only a member of both groups sees the link).
     */
    val canDelete: Boolean get() = true

    /** Expenses, incomes and account transfers are edited in the form; adjustments and contribution legs are not. */
    val canEdit: Boolean get() = !isGroupTransfer && kind != MovementKind.Adjustment
}

/** The kinds the Type filter offers; [wireName] is what `search_movements` expects in `p_kinds`. */
enum class MovementType(val wireName: String) {
    Expense("expense"),
    Income("income"),
    Transfer("transfer"),
    Adjustment("adjustment"),
    GroupTransfer("group_transfer"),
}

/**
 * What the Movements list is filtered by. An empty filter is the whole ledger. [accountId] is the fixed account of the
 * level 3 screen or the account chosen in the Account chip; a movement matches any of [tagIds]; [query] is the search
 * text (notes, or an amount such as `42,30`).
 */
data class MovementFilter(
    val accountId: String? = null,
    val envelopeId: String? = null,
    val type: MovementType? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val tagIds: Set<String> = emptySet(),
    val createdBy: String? = null,
    val query: String = "",
) {
    val hasSearch: Boolean get() = query.isNotBlank()
}

/** One page of the list, and how many rows the filter matches in all. */
data class MovementPage(val items: List<Movement>, val total: Long)

/** A free label of the group that movements can carry. */
data class Tag(val id: String, val groupId: String, val name: String, val color: String? = null)

/**
 * A tag with how many live (not voided) movements carry it, as `tag_usage` counts them. [archivedAt] is set for an
 * archived tag, which the Tags screen lists apart.
 */
data class TagUsage(
    val id: String,
    val name: String,
    val color: String?,
    val archivedAt: String?,
    val movements: Long,
) {
    val isArchived: Boolean get() = archivedAt != null

    fun toTag(groupId: String) = Tag(id, groupId, name, color)
}

/** Today (the group's time zone) and the group's periods, to turn "This period" and the like into dates. */
data class DateContext(val today: LocalDate, val currentPeriod: PeriodRange, val previousPeriod: PeriodRange?)
