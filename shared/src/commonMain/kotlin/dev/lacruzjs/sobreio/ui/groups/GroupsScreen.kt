package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.groups_empty_body
import dev.lacruzjs.sobreio.generated.resources.groups_empty_title
import dev.lacruzjs.sobreio.generated.resources.groups_title
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GroupsRoute(
    onOpenGroup: (groupId: String) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Every time the screen is shown, including when coming back from a group
    LaunchedEffect(Unit) { viewModel.onEvent(GroupsEvent.Refresh) }

    state.navigateToGroup?.let { groupId ->
        LaunchedEffect(groupId) {
            onOpenGroup(groupId)
            viewModel.onEvent(GroupsEvent.NavigationHandled)
        }
    }

    GroupsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                GroupsEvent.ProfileClicked -> onOpenProfile()
                is GroupsEvent.GroupClicked -> onOpenGroup(event.groupId)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun GroupsScreen(
    state: GroupsUiState,
    onEvent: (GroupsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.groups_title),
                actions = {
                    AddIconButton(
                        contentDescription = stringResource(Res.string.action_new_group),
                        onClick = { onEvent(GroupsEvent.NewGroupClicked) },
                    )
                },
            )
            // The list is the only part that scrolls
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.groups.isEmpty() && state.archivedGroups.isEmpty() && state.error != null -> EmptyState(
                        message = stringResource(state.error.messageRes()),
                    ) {
                        TextButton(onClick = { onEvent(GroupsEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                    }
                    state.groups.isEmpty() && state.archivedGroups.isEmpty() -> EmptyState(
                        title = stringResource(Res.string.groups_empty_title),
                        message = stringResource(Res.string.groups_empty_body),
                    )
                    else -> GroupList(state, onEvent)
                }
            }
            UndoSnackbarHost(
                message = state.undoMessage,
                onUndo = { onEvent(GroupsEvent.UndoClicked) },
                onDismiss = { onEvent(GroupsEvent.UndoExpired) },
            )
            GroupsBottomActions(state, onEvent)
        }
    }

    when (state.sheet) {
        GroupsSheet.None -> Unit
        GroupsSheet.Create -> GroupFormSheet(
            form = state.form,
            onChange = { onEvent(GroupsEvent.FormChanged(it)) },
            onConfirm = { onEvent(GroupsEvent.CreateConfirmed) },
            onDismiss = { onEvent(GroupsEvent.SheetDismissed) },
            isSubmitting = state.isSubmitting,
            submitError = state.submitError,
        )
        GroupsSheet.Join -> JoinGroupSheet(state, onEvent)
    }
}

private val previewGroups = listOf(
    Group("1", "Casa", "EUR", GroupRole.Owner),
    Group("2", "Viaje a Roma con un nombre muy largo", "USD", GroupRole.Member),
)

@PreviewLightDark
@Composable
private fun GroupsScreenPreview() {
    SobreioTheme { GroupsScreen(state = GroupsUiState(groups = previewGroups, isLoading = false), onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun GroupsScreenEmptyPreview() {
    SobreioTheme { GroupsScreen(state = GroupsUiState(isLoading = false), onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun GroupsScreenErrorPreview() {
    SobreioTheme { GroupsScreen(state = GroupsUiState(isLoading = false, error = AppError.Network), onEvent = {}) }
}
