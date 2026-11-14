package dev.lacruzjs.sobreio.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_deleted
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/** The accounts tab of one group. Currency and role come from the group the Route has already loaded. */
class AccountsViewModel(
    private val groupId: String,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    private val undo = UndoController(
        scope = viewModelScope,
        onMessage = { message -> _state.update { it.copy(undoMessage = message) } },
        onError = { error -> _state.update { it.copy(actionError = error) } },
    )

    fun onEvent(event: AccountsEvent) {
        when (event) {
            is AccountsEvent.GroupLoaded ->
                _state.update { it.copy(currencyCode = event.currencyCode, canEdit = event.canEdit) }
            AccountsEvent.Refresh -> {
                // A delete still waiting for its snackbar must be final before the list is read again
                undo.commit()
                load()
            }
            AccountsEvent.NewAccountClicked -> if (_state.value.canEdit) _state.update { it.openSheet(AccountsSheet.Create) }
            is AccountsEvent.EditClicked -> edit(event.accountId)
            is AccountsEvent.DeleteClicked -> delete(event.accountId)
            AccountsEvent.UndoClicked -> undo.undo()
            AccountsEvent.UndoExpired -> undo.commit()
            AccountsEvent.SheetDismissed ->
                _state.update { if (it.isSubmitting) it else it.copy(sheet = AccountsSheet.None, editingAccount = null) }
            is AccountsEvent.NameChanged -> _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is AccountsEvent.KindSelected -> _state.update { it.copy(kind = event.kind) }
            is AccountsEvent.OpeningBalanceChanged ->
                _state.update { it.copy(openingBalance = event.value, openingBalanceError = null, submitError = null) }
            AccountsEvent.ConfirmClicked -> confirm()
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    private fun load() {
        viewModelScope.launch {
            val list = accounts.getAccounts(groupId)
            val total = accounts.getAccountsTotal(groupId)
            val failure = list.exceptionOrNull() ?: total.exceptionOrNull()
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
            } else {
                _state.update {
                    it.copy(
                        accounts = list.getOrThrow(),
                        totalCents = total.getOrThrow(),
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    private fun edit(accountId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val account = current.accounts.firstOrNull { it.id == accountId } ?: return
        _state.update {
            it.openSheet(AccountsSheet.Edit).copy(editingAccount = account, name = account.name, kind = account.kind)
        }
    }

    private fun confirm() {
        val current = _state.value
        if (!current.canEdit || current.isSubmitting) return
        val name = current.name.trim()
        val nameError = validateAccountName(name)
        val editing = current.sheet == AccountsSheet.Edit
        val openingBalance = if (editing) 0L else parseMoneyInput(current.openingBalance)
        val balanceError = if (openingBalance == null) Res.string.validation_amount_invalid else null
        if (nameError != null || balanceError != null) {
            _state.update { it.copy(nameError = nameError, openingBalanceError = balanceError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (editing) {
                val account = current.editingAccount ?: return@launch
                accounts.updateAccount(account.id, name, current.kind)
            } else {
                accounts.createAccount(groupId, name, current.kind, openingBalance ?: 0L).map { }
            }
            result
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false, sheet = AccountsSheet.None, editingAccount = null) }
                    load()
                }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    /**
     * No confirmation: the account disappears at once and is archived right away, so a refusal (balance not zero,
     * live commitments) comes back immediately and the row returns. The snackbar's "Undo" unarchives it.
     */
    private fun delete(accountId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val account = current.accounts.firstOrNull { it.id == accountId } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update { it.copy(accounts = it.accounts - account, actionError = null) }
        val restore = {
            _state.update { it.copy(accounts = (it.accounts + account).sortedBy { a -> a.name.lowercase() }) }
            load()
        }
        viewModelScope.launch {
            accounts.archiveAccount(account.id)
                .onSuccess {
                    undo.show(
                        UndoableAction(
                            message = Res.string.account_deleted,
                            argument = account.name,
                            commit = { Result.success(Unit) },
                            undo = { accounts.unarchiveAccount(account.id) },
                            restore = restore,
                        ),
                    )
                }
                .onFailure { e ->
                    restore()
                    _state.update { it.copy(actionError = e.asAppError()) }
                }
        }
    }

    private fun validateAccountName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_ACCOUNT_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }

    private fun AccountsUiState.openSheet(sheet: AccountsSheet) = copy(
        sheet = sheet,
        editingAccount = null,
        name = "",
        nameError = null,
        kind = AccountKind.Bank,
        openingBalance = "",
        openingBalanceError = null,
        submitError = null,
    )
}
