package dev.lacruzjs.sobreio.ui.group

import dev.lacruzjs.sobreio.data.realtime.GroupChangeFeed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.error.asAppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.ui.loading.revalidate
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
    changes: GroupChangeFeed,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupUiState())
    val state: StateFlow<GroupUiState> = _state.asStateFlow()

    init {
        load()
        // While the group is open (its entry stays in the back stack under any detail), other members' changes arrive
        // live: the feed makes the cache stale and the screen on display reads again
        viewModelScope.launch { changes.follow(groupId).collect { refresh() } }
    }

    fun onEvent(event: GroupEvent) {
        when (event) {
            GroupEvent.Retry -> load()
            GroupEvent.NavigationHandled -> _state.update { it.copy(navigateBack = false) }
            GroupEvent.NewItemClicked -> _state.update { it.copy(newItemSheetOpen = true) }
            GroupEvent.NewItemDismissed -> _state.update { it.copy(newItemSheetOpen = false) }
            GroupEvent.Refresh -> refresh()
            // Handled by the Route
            GroupEvent.MenuClicked, is GroupEvent.TabSelected, GroupEvent.SettingsClicked -> Unit
        }
    }

    /** Reads the group again (its settings may have changed it): the cached one stays on screen meanwhile. */
    private fun refresh() {
        if (_state.value.group == null) return
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = it.group == null, error = null) }
        viewModelScope.launch {
            val userId = auth.authState.filterIsInstance<AuthState.SignedIn>().first().user.id
            revalidate(onRefreshing = { refreshing -> _state.update { it.copy(isRefreshing = refreshing) } }) { read(userId) }
        }
    }

    private suspend fun read(userId: String) {
        groups.getGroup(groupId)
            .onSuccess { group ->
                lastGroup.set(userId, groupId)
                _state.update { it.copy(group = group, isLoading = false, error = null) }
            }
            .onFailure { e ->
                val error = e.asAppError()
                when {
                    error == AppError.NotFound -> {
                        lastGroup.clear(userId)
                        _state.update { it.copy(isLoading = false, navigateBack = true) }
                    }
                    // A failed refresh keeps the group on screen; the tabs report their own failures
                    _state.value.group != null -> Unit
                    else -> _state.update { it.copy(isLoading = false, error = error) }
                }
            }
    }
}
