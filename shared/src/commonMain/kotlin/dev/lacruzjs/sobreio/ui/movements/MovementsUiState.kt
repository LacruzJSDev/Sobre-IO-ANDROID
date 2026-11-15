package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate

/** Which bottom sheet of the Movements screen is open: one per chip. */
enum class MovementsSheet { None, Account, Envelope, Type, Dates, Tags, Person }

data class MovementsUiState(
    val groupName: String = "",
    /** Money is formatted with it. */
    val currencyCode: String = "EUR",
    val canEdit: Boolean = false,
    /** The account of the level 3 screen: the list is always its own and the Account chip is not shown. Null on the tab. */
    val fixedAccountId: String? = null,
    val filter: MovementFilter = MovementFilter(),
    /** Which Dates option produced `filter.from` and `filter.to`; null when no dates are set. */
    val datePreset: DatePreset? = null,
    val searchOpen: Boolean = false,
    /** What the search field shows; [MovementFilter.query] follows it after a pause. */
    val searchText: String = "",
    /** Live and archived accounts and envelopes, to name the rows; options of the chips. */
    val accounts: List<Account> = emptyList(),
    val envelopes: List<Envelope> = emptyList(),
    val members: List<Member> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val dateContext: DateContext? = null,
    /** The pages loaded so far, newest first, minus the rows hidden while their delete waits for its snackbar. */
    val items: List<Movement> = emptyList(),
    val hasMore: Boolean = false,
    /** True while the first page of the current filter loads. */
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val loadMoreFailed: Boolean = false,
    /** Failure of loading the first page. */
    val error: AppError? = null,
    /** Failure of deleting or undoing, shown above the list. */
    val actionError: AppError? = null,
    val sheet: MovementsSheet = MovementsSheet.None,
    val customRangeOpen: Boolean = false,
    val undoMessage: UndoMessage? = null,
) {
    val fixedAccount: Account? get() = fixedAccountId?.let { id -> accounts.firstOrNull { it.id == id } }

    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()

    /** With one live account every row would repeat its name, so the second line leaves it out. */
    val hasSeveralAccounts: Boolean get() = accounts.count { !it.isArchived } > 1

    val today: LocalDate? get() = dateContext?.today

    /** A filter or a search is on (the fixed account does not count): the empty state then says "no results". */
    val hasActiveFilters: Boolean
        get() = filter.copy(accountId = if (fixedAccountId != null) null else filter.accountId) != MovementFilter()

    val sections: List<DaySection> get() = groupByDay(items, today)

    fun accountName(id: String?): String? = accounts.firstOrNull { it.id == id }?.name
    fun envelope(id: String?): Envelope? = envelopes.firstOrNull { it.id == id }
}
