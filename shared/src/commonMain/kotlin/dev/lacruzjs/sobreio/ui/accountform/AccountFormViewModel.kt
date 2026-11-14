package dev.lacruzjs.sobreio.ui.accountform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

const val MAX_ACCOUNT_NAME_LENGTH = 100

data class AccountFormUiState(
    val name: String = "",
    val nameError: StringResource? = null,
    val kind: AccountKind = AccountKind.Bank,
    /** Text of the opening balance field (create only). */
    val openingBalance: String = "",
    val openingBalanceError: StringResource? = null,
    /** Editing: the account is being read. */
    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** The account was created or saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
)

sealed interface AccountFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : AccountFormEvent
    data class NameChanged(val value: String) : AccountFormEvent
    data class KindSelected(val kind: AccountKind) : AccountFormEvent
    data class OpeningBalanceChanged(val value: String) : AccountFormEvent
    data object SaveClicked : AccountFormEvent
    data object Retry : AccountFormEvent
    data object NavigationHandled : AccountFormEvent
}

/**
 * The create and edit form of an account. [accountId] null creates (name, kind and opening balance); otherwise the
 * account is read and its name and kind can change (the opening balance exists only at creation).
 */
class AccountFormViewModel(
    private val groupId: String,
    private val accountId: String?,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountFormUiState(isLoading = accountId != null))
    val state: StateFlow<AccountFormUiState> = _state.asStateFlow()

    init {
        if (accountId != null) load(accountId)
    }

    fun onEvent(event: AccountFormEvent) {
        when (event) {
            is AccountFormEvent.NameChanged -> _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is AccountFormEvent.KindSelected -> _state.update { it.copy(kind = event.kind) }
            is AccountFormEvent.OpeningBalanceChanged ->
                _state.update { it.copy(openingBalance = event.value, openingBalanceError = null, submitError = null) }
            AccountFormEvent.SaveClicked -> save()
            AccountFormEvent.Retry -> accountId?.let { load(it) }
            AccountFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            AccountFormEvent.CloseClicked -> Unit
        }
    }

    private fun load(id: String) {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            accounts.getAccounts(groupId)
                .onSuccess { list ->
                    val account = list.firstOrNull { it.id == id }
                    if (account == null) {
                        _state.update { it.copy(isLoading = false, loadError = AppError.NotFound) }
                    } else {
                        _state.update { it.copy(isLoading = false, name = account.name, kind = account.kind) }
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, loadError = e.asAppError()) } }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSubmitting || current.isLoading || current.loadError != null) return
        val name = current.name.trim()
        val nameError = validateName(name)
        val editing = accountId != null
        val openingBalance = if (editing) 0L else parseMoneyInput(current.openingBalance)
        val balanceError = if (openingBalance == null) Res.string.validation_amount_invalid else null
        if (nameError != null || balanceError != null) {
            _state.update { it.copy(nameError = nameError, openingBalanceError = balanceError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = if (accountId != null) {
                accounts.updateAccount(accountId, name, current.kind)
            } else {
                accounts.createAccount(groupId, name, current.kind, openingBalance ?: 0L).map { }
            }
            result
                .onSuccess { _state.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) } }
        }
    }

    private fun validateName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_ACCOUNT_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }
}
