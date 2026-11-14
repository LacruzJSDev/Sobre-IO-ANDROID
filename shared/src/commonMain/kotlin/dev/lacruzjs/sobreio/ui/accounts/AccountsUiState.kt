package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.ui.undo.UndoMessage

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    /** Total of the group's accounts, as the database computes it. */
    val totalCents: Long = 0,
    /** Set by [AccountsEvent.GroupLoaded]; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin: they can create, edit and delete (the Route shows the "+" and the swipe). Members only read. */
    val canEdit: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading the list. */
    val error: AppError? = null,
    /** Failure of deleting or undoing, shown above the list. */
    val actionError: AppError? = null,
    /** Snackbar with "Undo" for the last delete. */
    val undoMessage: UndoMessage? = null,
)
