package dev.lacruzjs.sobreio.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the open group. A group that loads becomes the user's last group; one that does not exist any more (or the
 * user left) is forgotten and the screen asks to go back to Groups. Other failures keep the stored group.
 */
class GroupViewModel(
    private val groupId: String,
    private val groups: GroupRepository,
    private val lastGroup: LastGroupStore,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupUiState())
    val state: StateFlow<GroupUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: GroupEvent) {
        when (event) {
            GroupEvent.Retry -> load()
            GroupEvent.NavigationHandled -> _state.update { it.copy(navigateBack = false) }
            GroupEvent.NewItemClicked -> _state.update { it.copy(newItemSheetOpen = true) }
            GroupEvent.NewItemDismissed -> _state.update { it.copy(newItemSheetOpen = false) }
            GroupEvent.InviteClicked -> invite()
            GroupEvent.InviteDismissed -> _state.update { it.copy(inviteSheetOpen = false) }
            // Handled by the Route
            GroupEvent.BackClicked, is GroupEvent.TabSelected -> Unit
        }
    }

    private fun invite() {
        if (_state.value.group?.canEdit != true || _state.value.isInviting) return
        _state.update { it.copy(inviteSheetOpen = true, isInviting = true, invitation = null, inviteError = null) }
        viewModelScope.launch {
            groups.createInvitation(groupId)
                .onSuccess { invitation -> _state.update { it.copy(isInviting = false, invitation = invitation) } }
                .onFailure { e -> _state.update { it.copy(isInviting = false, inviteError = e.asAppError()) } }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val userId = auth.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
            groups.getGroup(groupId)
                .onSuccess { group ->
                    lastGroup.set(userId, groupId)
                    _state.update { it.copy(group = group, isLoading = false) }
                }
                .onFailure { e ->
                    val error = e.asAppError()
                    if (error == AppError.NotFound) {
                        lastGroup.clear(userId)
                        _state.update { it.copy(isLoading = false, navigateBack = true) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = error) }
                    }
                }
        }
    }
}
