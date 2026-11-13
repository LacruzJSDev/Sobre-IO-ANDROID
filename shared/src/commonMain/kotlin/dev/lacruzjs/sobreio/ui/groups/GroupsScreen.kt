package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create
import dev.lacruzjs.sobreio.generated.resources.action_join
import dev.lacruzjs.sobreio.generated.resources.action_join_with_code
import dev.lacruzjs.sobreio.generated.resources.action_new_group
import dev.lacruzjs.sobreio.generated.resources.action_profile_and_settings
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.field_currency
import dev.lacruzjs.sobreio.generated.resources.field_group_name
import dev.lacruzjs.sobreio.generated.resources.field_invitation_code
import dev.lacruzjs.sobreio.generated.resources.groups_empty_body
import dev.lacruzjs.sobreio.generated.resources.groups_empty_title
import dev.lacruzjs.sobreio.generated.resources.groups_title
import dev.lacruzjs.sobreio.generated.resources.ic_add
import dev.lacruzjs.sobreio.generated.resources.ic_settings
import dev.lacruzjs.sobreio.generated.resources.join_group_body
import dev.lacruzjs.sobreio.generated.resources.join_group_title
import dev.lacruzjs.sobreio.generated.resources.new_group_currency_note
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.GroupAvatar
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.painterResource
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.groups_title),
                actions = {
                    IconButton(onClick = { onEvent(GroupsEvent.ProfileClicked) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = stringResource(Res.string.action_profile_and_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEvent(GroupsEvent.NewGroupClicked) },
                icon = { Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(Res.string.action_new_group)) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .fillMaxSize(),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.groups.isEmpty() && state.error != null -> EmptyState(
                    message = stringResource(state.error.messageRes()),
                ) {
                    TextButton(onClick = { onEvent(GroupsEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                state.groups.isEmpty() -> EmptyState(
                    title = stringResource(Res.string.groups_empty_title),
                    message = stringResource(Res.string.groups_empty_body),
                ) {
                    TextButton(onClick = { onEvent(GroupsEvent.JoinClicked) }) {
                        Text(stringResource(Res.string.action_join_with_code))
                    }
                }
                else -> GroupList(state.groups, onEvent)
            }
        }
    }

    when (state.sheet) {
        GroupsSheet.None -> Unit
        GroupsSheet.Create -> CreateGroupSheet(state, onEvent)
        GroupsSheet.Join -> JoinGroupSheet(state, onEvent)
    }
}

@Composable
private fun GroupList(groups: List<Group>, onEvent: (GroupsEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room under the last row so the floating button never covers it
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(groups, key = { it.id }) { group ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEvent(GroupsEvent.GroupClicked(group.id)) }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroupAvatar(name = group.name)
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = group.currencyCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            TextButton(onClick = { onEvent(GroupsEvent.JoinClicked) }) {
                Text(stringResource(Res.string.action_join_with_code))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(GroupsEvent.SheetDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(title = stringResource(Res.string.action_new_group)) {
            FormTextField(
                value = state.name,
                onValueChange = { onEvent(GroupsEvent.GroupNameChanged(it)) },
                label = stringResource(Res.string.field_group_name),
                errorRes = state.nameError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(GroupsEvent.CreateConfirmed) },
            )
            Text(text = stringResource(Res.string.field_currency), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCurrencies.forEach { code ->
                    FilterChip(
                        selected = state.currency == code,
                        onClick = { onEvent(GroupsEvent.CurrencySelected(code)) },
                        enabled = !state.isSubmitting,
                        label = { Text(code) },
                    )
                }
            }
            Text(
                text = stringResource(Res.string.new_group_currency_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            LoadingButton(
                text = stringResource(Res.string.action_create),
                onClick = { onEvent(GroupsEvent.CreateConfirmed) },
                isLoading = state.isSubmitting,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun JoinGroupSheet(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(GroupsEvent.SheetDismissed) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(title = stringResource(Res.string.join_group_title)) {
            Text(
                text = stringResource(Res.string.join_group_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FormTextField(
                value = state.code,
                onValueChange = { onEvent(GroupsEvent.CodeChanged(it)) },
                label = stringResource(Res.string.field_invitation_code),
                errorRes = state.codeError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done,
                onImeAction = { onEvent(GroupsEvent.JoinConfirmed) },
            )
            ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
            LoadingButton(
                text = stringResource(Res.string.action_join),
                onClick = { onEvent(GroupsEvent.JoinConfirmed) },
                isLoading = state.isSubmitting,
            )
        }
    }
}

@Composable
private fun SheetColumn(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

private val previewGroups = listOf(Group("1", "Casa", "EUR"), Group("2", "Viaje a Roma con un nombre muy largo", "USD"))

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
