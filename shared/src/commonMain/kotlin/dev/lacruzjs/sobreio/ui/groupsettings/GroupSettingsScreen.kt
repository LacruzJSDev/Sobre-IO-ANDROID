package dev.lacruzjs.sobreio.ui.groupsettings
import dev.lacruzjs.sobreio.ui.realtime.RefreshOnRemoteChange
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.lacruzjs.sobreio.ui.components.ContentWidth
import dev.lacruzjs.sobreio.ui.components.SingleColumnPadding
import dev.lacruzjs.sobreio.ui.components.RightColumnPadding
import dev.lacruzjs.sobreio.ui.components.LeftColumnPadding
import dev.lacruzjs.sobreio.ui.components.TwoColumnSplit
import dev.lacruzjs.sobreio.ui.components.DetailSkeleton
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_invite
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.group_settings_title
import dev.lacruzjs.sobreio.generated.resources.invitations_empty
import dev.lacruzjs.sobreio.generated.resources.member_former
import dev.lacruzjs.sobreio.generated.resources.settings_section_accepted
import dev.lacruzjs.sobreio.generated.resources.settings_section_pending
import dev.lacruzjs.sobreio.generated.resources.settings_section_invitations
import dev.lacruzjs.sobreio.generated.resources.settings_section_members
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.changeRoleAction
import dev.lacruzjs.sobreio.ui.components.removeMemberAction
import dev.lacruzjs.sobreio.ui.components.revokeAction
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Composable
fun GroupSettingsRoute(
    groupId: String,
    onBack: () -> Unit,
    onBackToGroups: () -> Unit,
    onEditGroup: () -> Unit,
    /** Opens the Tags screen; true opens it with the create sheet (the "+" of an empty section). */
    onOpenTags: (startWithCreate: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Shown again after the group form: read the group, members and invitations again
    LaunchedEffect(Unit) { viewModel.onEvent(GroupSettingsEvent.Refresh) }
    // Changes made elsewhere in the group are read as they happen (D-55)
    RefreshOnRemoteChange(paused = state.undoMessage != null) { viewModel.onEvent(GroupSettingsEvent.Refresh) }

    if (state.navigateToGroups) {
        LaunchedEffect(Unit) {
            onBackToGroups()
            viewModel.onEvent(GroupSettingsEvent.NavigationHandled)
        }
    }

    GroupSettingsScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                GroupSettingsEvent.BackClicked -> onBack()
                GroupSettingsEvent.EditClicked -> onEditGroup()
                GroupSettingsEvent.TagsClicked -> onOpenTags(false)
                GroupSettingsEvent.NewTagClicked -> onOpenTags(true)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun GroupSettingsScreen(
    state: GroupSettingsUiState,
    onEvent: (GroupSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().contentMaxWidth(ContentWidth.TwoColumns)) {
            ScreenHeader(
                isRefreshing = state.isRefreshing,
                title = stringResource(Res.string.group_settings_title),
                onBack = { onEvent(GroupSettingsEvent.BackClicked) },
                actions = {
                    // Same place as the gear that opened this screen: the screen's one action, top right
                    if (state.group != null && state.canEditDetails) {
                        IconActionButton(
                            icon = Icons.Filled.Edit,
                            label = stringResource(Res.string.edit_group_title),
                            onClick = { onEvent(GroupSettingsEvent.EditClicked) },
                        )
                    }
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> DetailSkeleton()
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(GroupSettingsEvent.Retry) }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                    state.group != null -> SettingsContent(state.group, state, onEvent)
                }
            }
            UndoSnackbarHost(
                message = state.undoMessage,
                onUndo = { onEvent(GroupSettingsEvent.UndoClicked) },
                onDismiss = { onEvent(GroupSettingsEvent.UndoExpired) },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }

    when (state.sheet) {
        GroupSettingsSheet.None -> Unit
        GroupSettingsSheet.Invite -> InviteSheet(state, onEvent)
        GroupSettingsSheet.Role -> RoleSheet(state, onEvent)
    }
}

@Composable
private fun SettingsContent(group: Group, state: GroupSettingsUiState, onEvent: (GroupSettingsEvent) -> Unit) {
    // Wide pane: what the group is (details and tags) on the left, who is in it (members and invitations) on the right
    TwoColumnSplit(
        narrow = { SettingsList(group, state, onEvent, withSummary = true, contentPadding = SingleColumnPadding) },
        left = {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LeftColumnPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DetailsSection(group)
                TagsSection(state.tags, state.canManageTags, onEvent)
            }
        },
        right = { SettingsList(group, state, onEvent, withSummary = false, contentPadding = RightColumnPadding) },
    )
}

@Composable
private fun SettingsList(
    group: Group,
    state: GroupSettingsUiState,
    onEvent: (GroupSettingsEvent) -> Unit,
    withSummary: Boolean,
    contentPadding: PaddingValues,
) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openKey = null }

    val languageTag = Locale.current.toLanguageTag()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (withSummary) item(key = "details") { DetailsSection(group) }

        if (state.actionError != null) {
            item(key = "error") {
                ErrorText(state.actionError, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        item(key = "members-title") { SectionTitle(stringResource(Res.string.settings_section_members)) }
        items(state.members, key = { "member-${it.userId}" }) { member ->
            val isYou = member.userId == state.currentUserId
            val row: @Composable () -> Unit = { MemberRow(member, isYou) }
            if (state.canManageMembers) {
                SwipeActionsRow(
                    actions = buildList {
                        add(changeRoleAction { onEvent(GroupSettingsEvent.RoleClicked(member.userId)) })
                        // Your own row has no Remove
                        if (!isYou) add(removeMemberAction { onEvent(GroupSettingsEvent.RemoveMemberClicked(member.userId)) })
                    },
                    isOpen = openKey == "member-${member.userId}",
                    onOpenChange = { open ->
                        val key = "member-${member.userId}"
                        if (open) openKey = key else if (openKey == key) openKey = null
                    },
                    onClick = {},
                    content = row,
                )
            } else {
                row()
            }
        }

        if (withSummary) item(key = "tags") { TagsSection(state.tags, state.canManageTags, onEvent) }

        if (state.canInvite) {
            item(key = "invitations-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(Res.string.settings_section_invitations), Modifier.weight(1f))
                    AddIconButton(
                        contentDescription = stringResource(Res.string.action_invite),
                        onClick = { onEvent(GroupSettingsEvent.InviteClicked) },
                    )
                }
            }
            if (state.pendingInvitations.isEmpty() && state.acceptedInvitations.isEmpty()) {
                item(key = "invitations-empty") {
                    Text(
                        text = stringResource(Res.string.invitations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.pendingInvitations.isNotEmpty()) {
                item(key = "pending-title") { SectionTitle(stringResource(Res.string.settings_section_pending)) }
            }
            items(state.pendingInvitations, key = { "invitation-${it.id}" }) { invitation ->
                SwipeActionsRow(
                    actions = listOf(revokeAction { onEvent(GroupSettingsEvent.RevokeInvitationClicked(invitation.id)) }),
                    isOpen = openKey == "invitation-${invitation.id}",
                    onOpenChange = { open ->
                        val key = "invitation-${invitation.id}"
                        if (open) openKey = key else if (openKey == key) openKey = null
                    },
                    onClick = {},
                ) {
                    PendingInvitationRow(invitation, languageTag)
                }
            }
            if (state.acceptedInvitations.isNotEmpty()) {
                item(key = "accepted-title") { SectionTitle(stringResource(Res.string.settings_section_accepted)) }
            }
            items(state.acceptedInvitations, key = { "accepted-${it.id}" }) { invitation ->
                AcceptedInvitationRow(
                    invitation = invitation,
                    name = state.members.firstOrNull { it.userId == invitation.acceptedBy }?.displayName
                        ?: stringResource(Res.string.member_former),
                    languageTag = languageTag,
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 4.dp),
    )
}

private val previewGroup = Group("1", "Casa", "EUR", GroupRole.Owner, color = "#3B6E8F", icon = "home", periodStartDay = 5)

@PreviewLightDark
@Composable
private fun GroupSettingsScreenPreview() {
    SobreioTheme {
        GroupSettingsScreen(
            state = GroupSettingsUiState(
                group = previewGroup,
                currentUserId = "u1",
                members = listOf(
                    Member("u1", "Ana", GroupRole.Owner),
                    Member("u2", "Marta", GroupRole.Admin),
                    Member("u3", "Luis", GroupRole.Member),
                ),
                pendingInvitations = listOf(Invitation("i1", "A1B2C3D4E5F6", GroupRole.Member, Instant.parse("2026-12-01T10:00:00Z"))),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
