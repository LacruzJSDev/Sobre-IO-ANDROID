package dev.lacruzjs.sobreio.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.account_deleted
import dev.lacruzjs.sobreio.generated.resources.account_reopened
import dev.lacruzjs.sobreio.ui.undo.UndoController
import dev.lacruzjs.sobreio.ui.undo.UndoableAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** The accounts tab of one group. Currency and role come from the group the Route has already loaded. */
class AccountsViewModel(
    private val groupId: String,
    private val accounts: AccountRepository,
    private val members: MemberRepository,
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
            is AccountsEvent.DeleteClicked -> delete(event.accountId)
            is AccountsEvent.ReopenClicked -> reopen(event.accountId)
            AccountsEvent.UndoClicked -> undo.undo()
            AccountsEvent.UndoExpired -> undo.commit()
            // Handled by the Route, which opens the form
            is AccountsEvent.EditClicked -> Unit
        }
    }

    override fun onCleared() {
        // viewModelScope is cancelled by now; a pending action still has to be made final
        undo.commitDetached()
    }

    private fun load() {
        viewModelScope.launch {
            val list = accounts.getAccounts(groupId)
            val summary = accounts.getSummary(groupId)
            val failure = list.exceptionOrNull() ?: summary.exceptionOrNull()
            if (failure != null) {
                _state.update { it.copy(isLoading = false, error = failure.asAppError()) }
            } else {
                val (archived, live) = list.getOrThrow().partition { it.isArchived }
                val figures = summary.getOrThrow()
                _state.update {
                    it.copy(
                        accounts = live.sortedByName(),
                        archivedAccounts = archived.sortedByName(),
                        totalCents = figures.totalCents,
                        inEnvelopesCents = figures.inEnvelopesCents,
                        outsideCents = figures.outsideCents,
                        isLoading = false,
                        error = null,
                    )
                }
            }
            // Only needed to name holders; without them the rows simply show no holder
            members.getMembers(groupId).onSuccess { list -> _state.update { it.copy(members = list) } }
        }
    }

    private fun List<Account>.sortedByName() = sortedBy { it.name.lowercase() }

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
        val archived = account.copy(archivedAt = Clock.System.now().toString())
        _state.update {
            it.copy(
                accounts = it.accounts - account,
                archivedAccounts = (it.archivedAccounts + archived).sortedByName(),
                actionError = null,
            )
        }
        val restore = {
            _state.update {
                it.copy(
                    accounts = (it.accounts + account).sortedByName(),
                    archivedAccounts = it.archivedAccounts - archived,
                )
            }
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

    /** Owner or admin of an archived account: it becomes live again and moves to the main list; "Undo" archives it again. */
    private fun reopen(accountId: String) {
        val current = _state.value
        if (!current.canEdit) return
        val account = current.archivedAccounts.firstOrNull { it.id == accountId } ?: return
        // Only one destructive action at a time: the previous one becomes final
        undo.commit()
        _state.update { it.copy(actionError = null) }
        viewModelScope.launch {
            accounts.unarchiveAccount(account.id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            accounts = (it.accounts + account.copy(archivedAt = null)).sortedByName(),
                            archivedAccounts = it.archivedAccounts - account,
                        )
                    }
                    undo.show(
                        UndoableAction(
                            message = Res.string.account_reopened,
                            argument = account.name,
                            commit = { Result.success(Unit) },
                            undo = { accounts.archiveAccount(account.id) },
                            restore = { load() },
                        ),
                    )
                }
                .onFailure { e -> _state.update { it.copy(actionError = e.asAppError()) } }
        }
    }
}
