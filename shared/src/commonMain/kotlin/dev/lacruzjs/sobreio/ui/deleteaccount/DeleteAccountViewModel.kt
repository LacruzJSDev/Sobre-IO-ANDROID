package dev.lacruzjs.sobreio.ui.deleteaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.repository.AccountDeletionRepository
import dev.lacruzjs.sobreio.domain.error.asAppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shows what deleting the account does to each group and deletes it. The repository closes the session on success, so
 * the root navigation swaps to the sign-in screens by itself; nothing here navigates.
 */
class DeleteAccountViewModel(
    private val deletion: AccountDeletionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountUiState())
    val state: StateFlow<DeleteAccountUiState> = _state.asStateFlow()

    init {
        load(showSpinner = true)
    }

    fun onEvent(event: DeleteAccountEvent) {
        when (event) {
            DeleteAccountEvent.Retry -> load(showSpinner = true)
            DeleteAccountEvent.Refresh -> if (_state.value.items != null) load(showSpinner = false)
            is DeleteAccountEvent.ConfirmChanged -> _state.update { it.copy(confirmed = event.checked, deleteError = null) }
            DeleteAccountEvent.DeleteClicked -> delete()
            // Handled by the Route, which owns the back stack
            DeleteAccountEvent.BackClicked, is DeleteAccountEvent.BlockedGroupClicked -> Unit
        }
    }

    private fun load(showSpinner: Boolean) {
        if (showSpinner) _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            deletion.getPreview()
                .onSuccess { items -> _state.update { it.copy(isLoading = false, items = items, loadError = null) } }
                .onFailure { e ->
                    // A failed silent refresh keeps what is shown
                    if (showSpinner) _state.update { it.copy(isLoading = false, loadError = e.asAppError()) }
                }
        }
    }

    private fun delete() {
        val current = _state.value
        if (!current.canDelete) return
        _state.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            deletion.deleteAccount()
                .onSuccess { _state.update { it.copy(isDeleting = false) } }
                .onFailure { e ->
                    _state.update { it.copy(isDeleting = false, deleteError = e.asAppError()) }
                    // A rule can have changed since the preview (someone left a group): read it again
                    load(showSpinner = false)
                }
        }
    }
}
