package dev.lacruzjs.sobreio.ui.accountform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AccountDraft
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.hasSeveralMembers
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_account_scope_movements
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
    /** Hex of the chosen palette colour; null is the brand ink. */
    val color: String? = null,
    /** Key of the chosen icon of the account catalogue; null is none. */
    val icon: String? = null,
    /** Holder: a member's user id, or null for a joint account. */
    val ownerUserId: String? = null,
    /** Whether the account shares its money into the envelopes. */
    val inEnvelopes: Boolean = true,
    /** Text of the opening balance field (create only). */
    val openingBalance: String = "",
    val openingBalanceError: StringResource? = null,
    /** The active members: they are the holders to choose from. */
    val members: List<Member> = emptyList(),
    /** Editing: the account is being read. */
    val isLoading: Boolean = false,
    val loadError: AppError? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
    /** The scope change was refused because the account has movements from today on; shown under the switch. */
    val scopeError: StringResource? = null,
    /** The account was created or saved: the Route leaves the form and consumes this. */
    val saved: Boolean = false,
) {
    /** The holder is only asked for when there is someone to choose between. */
    val hasSeveralMembers: Boolean get() = members.hasSeveralMembers()
}

sealed interface AccountFormEvent {
    /** Handled by the Route, which owns the back stack. */
    data object CloseClicked : AccountFormEvent
    data class NameChanged(val value: String) : AccountFormEvent
    data class KindSelected(val kind: AccountKind) : AccountFormEvent
    data class ColorSelected(val hex: String?) : AccountFormEvent
    data class IconSelected(val key: String?) : AccountFormEvent
    data class HolderSelected(val userId: String?) : AccountFormEvent
    data class InEnvelopesChanged(val value: Boolean) : AccountFormEvent
    data class OpeningBalanceChanged(val value: String) : AccountFormEvent
    data object SaveClicked : AccountFormEvent
    data object Retry : AccountFormEvent
    data object NavigationHandled : AccountFormEvent
}

/**
 * The create and edit form of an account. [accountId] null creates (every field and the opening balance go to
 * `create_account` in one call); otherwise the account is read and saved: first the scope when its switch changed
 * (`change_account_scope`, an event dated today), then name, kind, holder, colour and icon.
 */
class AccountFormViewModel(
    private val groupId: String,
    private val accountId: String?,
    private val accounts: AccountRepository,
    private val members: MemberRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountFormUiState(isLoading = accountId != null))
    val state: StateFlow<AccountFormUiState> = _state.asStateFlow()

    /** The scope the database has; the switch is compared with it to know whether to call `change_account_scope`. */
    private var storedInEnvelopes = true

    init {
        loadMembers()
        if (accountId != null) load(accountId)
    }

    fun onEvent(event: AccountFormEvent) {
        when (event) {
            is AccountFormEvent.NameChanged -> _state.update { it.copy(name = event.value, nameError = null, submitError = null) }
            is AccountFormEvent.KindSelected -> _state.update { it.copy(kind = event.kind) }
            is AccountFormEvent.ColorSelected -> _state.update { it.copy(color = event.hex) }
            is AccountFormEvent.IconSelected -> _state.update { it.copy(icon = event.key) }
            is AccountFormEvent.HolderSelected -> _state.update { it.copy(ownerUserId = event.userId) }
            is AccountFormEvent.InEnvelopesChanged ->
                _state.update { it.copy(inEnvelopes = event.value, scopeError = null, submitError = null) }
            is AccountFormEvent.OpeningBalanceChanged ->
                _state.update { it.copy(openingBalance = event.value, openingBalanceError = null, submitError = null) }
            AccountFormEvent.SaveClicked -> save()
            AccountFormEvent.Retry -> accountId?.let { load(it) }
            AccountFormEvent.NavigationHandled -> _state.update { it.copy(saved = false) }
            AccountFormEvent.CloseClicked -> Unit
        }
    }

    /** A failure only hides the holder choice: the form works without it. */
    private fun loadMembers() {
        viewModelScope.launch {
            members.getMembers(groupId).onSuccess { list -> _state.update { it.copy(members = list) } }
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
                        storedInEnvelopes = account.inEnvelopes
                        _state.update {
                            it.copy(
                                isLoading = false,
                                name = account.name,
                                kind = account.kind,
                                color = account.color,
                                icon = account.icon,
                                ownerUserId = account.ownerUserId,
                                inEnvelopes = account.inEnvelopes,
                            )
                        }
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
        _state.update { it.copy(isSubmitting = true, submitError = null, scopeError = null) }
        val draft = AccountDraft(
            name = name,
            kind = current.kind,
            ownerUserId = current.ownerUserId,
            color = current.color,
            icon = current.icon,
        )
        viewModelScope.launch {
            if (accountId == null) {
                accounts.createAccount(groupId, draft, current.inEnvelopes, openingBalance ?: 0L)
                    .onSuccess { finish() }
                    .onFailure { e -> fail(e) }
                return@launch
            }
            // The scope goes first: it is the only step the database can refuse for a reason the user can fix
            if (current.inEnvelopes != storedInEnvelopes) {
                val scope = accounts.changeAccountScope(accountId, current.inEnvelopes)
                scope.onFailure { e ->
                    val error = e.asAppError()
                    if (error is AppError.BusinessRule && error.code == SCOPE_REFUSED) {
                        _state.update { it.copy(isSubmitting = false, scopeError = Res.string.error_account_scope_movements) }
                    } else {
                        fail(error)
                    }
                }
                if (scope.isFailure) return@launch
                // Done in the database: a retry of the rest of the form must not repeat it
                storedInEnvelopes = current.inEnvelopes
            }
            accounts.updateAccount(accountId, draft)
                .onSuccess { finish() }
                .onFailure { e -> fail(e) }
        }
    }

    private fun finish() = _state.update { it.copy(isSubmitting = false, saved = true) }

    private fun fail(e: Throwable) = _state.update { it.copy(isSubmitting = false, submitError = e.asAppError()) }

    private fun validateName(name: String): StringResource? = when {
        name.isEmpty() -> validateRequired(name)
        name.length > MAX_ACCOUNT_NAME_LENGTH -> Res.string.validation_name_too_long
        else -> null
    }

    private companion object {
        /** SQLSTATE of "the account has movements from today on" (and of other incoherent scopes). */
        const val SCOPE_REFUSED = "FZ017"
    }
}
