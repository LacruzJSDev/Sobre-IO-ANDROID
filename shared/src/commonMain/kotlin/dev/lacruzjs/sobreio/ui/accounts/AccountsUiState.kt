package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.ui.undo.UndoMessage

data class AccountsUiState(
    /** The live accounts, ordered by name. */
    val accounts: List<Account> = emptyList(),
    /** The archived accounts, ordered by name: the collapsed "Archived" section at the end of the list. */
    val archivedAccounts: List<Account> = emptyList(),
    /** Total of the group's accounts, as the database computes it. */
    val totalCents: Long = 0,
    /** Money in the accounts that share it into the envelopes, as the database computes it. */
    val inEnvelopesCents: Long = 0,
    /** Money in the accounts outside the envelopes, as the database computes it. */
    val outsideCents: Long = 0,
    /** The active members: holder names are shown only when there are several. */
    val members: List<Member> = emptyList(),
    /** Set by [AccountsEvent.GroupLoaded]; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin: they can create, edit, delete and reopen (the Route shows the "+" and the swipe). Members only read. */
    val canEdit: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading the list. */
    val error: AppError? = null,
    /** Failure of deleting, reopening or undoing, shown above the list. */
    val actionError: AppError? = null,
    /** Snackbar for the last delete or reopen. */
    val undoMessage: UndoMessage? = null,
) {
    /** Accounts whose money is distributed among the envelopes. */
    val inEnvelopes: List<Account> get() = accounts.filter { it.inEnvelopes }

    /** Accounts whose money is never distributed (savings, investments). */
    val outsideEnvelopes: List<Account> get() = accounts.filterNot { it.inEnvelopes }

    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()

    /** Name of the holder of [account], or null for a joint account, when the group has one holder to tell apart. */
    fun holderName(account: Account): String? {
        if (!hasSeveralMembers) return null
        return members.firstOrNull { it.userId == account.ownerUserId }?.displayName
    }
}
