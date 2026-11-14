package dev.lacruzjs.sobreio.ui.accounts

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import org.jetbrains.compose.resources.StringResource

const val MAX_ACCOUNT_NAME_LENGTH = 100

/** Which bottom sheet of the Accounts tab is open. */
enum class AccountsSheet { None, Create, Edit }

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    /** Total of the group's accounts, as the database computes it. */
    val totalCents: Long = 0,
    /** Set by [AccountsEvent.GroupLoaded]; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin: they can create, edit and delete. Members only read. */
    val canEdit: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading the list. */
    val error: AppError? = null,
    val sheet: AccountsSheet = AccountsSheet.None,
    val editingAccount: Account? = null,
    val name: String = "",
    val nameError: StringResource? = null,
    val kind: AccountKind = AccountKind.Bank,
    /** Text of the opening balance field (create only). */
    val openingBalance: String = "",
    val openingBalanceError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of creating or editing, shown inside the open sheet. */
    val submitError: AppError? = null,
    /** Failure of deleting or undoing, shown above the list. */
    val actionError: AppError? = null,
    /** Snackbar with "Undo" for the last delete. */
    val undoMessage: UndoMessage? = null,
)
