package dev.lacruzjs.sobreio.ui.groupsettings

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_archive_group
import dev.lacruzjs.sobreio.generated.resources.action_delete_group
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.action_invite
import dev.lacruzjs.sobreio.generated.resources.action_leave_group
import dev.lacruzjs.sobreio.generated.resources.action_remove
import dev.lacruzjs.sobreio.generated.resources.action_reopen_group
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.action_revoke
import dev.lacruzjs.sobreio.generated.resources.action_role
import dev.lacruzjs.sobreio.generated.resources.group_archived_banner
import dev.lacruzjs.sobreio.generated.resources.group_settings_title
import dev.lacruzjs.sobreio.generated.resources.invitation_expired
import dev.lacruzjs.sobreio.generated.resources.invitation_expires_on
import dev.lacruzjs.sobreio.generated.resources.invitation_summary
import dev.lacruzjs.sobreio.generated.resources.invitations_empty
import dev.lacruzjs.sobreio.generated.resources.member_name_you
import dev.lacruzjs.sobreio.generated.resources.settings_period_summary
import dev.lacruzjs.sobreio.generated.resources.settings_section_details
import dev.lacruzjs.sobreio.generated.resources.settings_section_invitations
import dev.lacruzjs.sobreio.generated.resources.settings_section_members
import dev.lacruzjs.sobreio.generated.resources.settings_time_zone_summary
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.DestructiveButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.SwipeAction
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.groups.GroupFormSheet
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun GroupSettingsRoute(
    groupId: String,
    onBack: () -> Unit,
    onBackToGroups: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingsViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.group_settings_title),
                onBack = { onEvent(GroupSettingsEvent.BackClicked) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
        GroupSettingsSheet.Edit -> GroupFormSheet(
            form = state.form,
            onChange = { onEvent(GroupSettingsEvent.FormChanged(it)) },
            onConfirm = { onEvent(GroupSettingsEvent.EditConfirmed) },
            onDismiss = { onEvent(GroupSettingsEvent.SheetDismissed) },
            isSubmitting = state.isSubmitting,
            submitError = state.submitError,
        )
        GroupSettingsSheet.Invite -> InviteSheet(state, onEvent)
        GroupSettingsSheet.Role -> RoleSheet(state, onEvent)
    }
}

@Composable
private fun SettingsContent(group: Group, state: GroupSettingsUiState, onEvent: (GroupSettingsEvent) -> Unit) {
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openKey = null }

    val languageTag = Locale.current.toLanguageTag()
    val now = remember { Clock.System.now() }
    val roleLabel = stringResource(Res.string.action_role)
    val removeLabel = stringResource(Res.string.action_remove)
    val revokeLabel = stringResource(Res.string.action_revoke)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "details") { DetailsSection(group, state.canEditDetails, onEvent) }

        item(key = "members-title") { SectionTitle(stringResource(Res.string.settings_section_members)) }
        items(state.members, key = { "member-${it.userId}" }) { member ->
            val isYou = member.userId == state.currentUserId
            val row: @Composable () -> Unit = { MemberRow(member, isYou) }
            if (state.canManageMembers) {
                SwipeActionsRow(
                    actions = buildList {
                        add(
                            SwipeAction(
                                label = roleLabel,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = { onEvent(GroupSettingsEvent.RoleClicked(member.userId)) },
                            ),
                        )
                        // Your own row has no Remove: that is "Leave group"
                        if (!isYou) {
                            add(
                                SwipeAction(
                                    label = removeLabel,
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    onClick = { onEvent(GroupSettingsEvent.RemoveMemberClicked(member.userId)) },
                                ),
                            )
                        }
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
            if (state.invitations.isEmpty()) {
                item(key = "invitations-empty") {
                    Text(
                        text = stringResource(Res.string.invitations_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.invitations, key = { "invitation-${it.id}" }) { invitation ->
                SwipeActionsRow(
                    actions = listOf(
                        SwipeAction(
                            label = revokeLabel,
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            onClick = { onEvent(GroupSettingsEvent.RevokeInvitationClicked(invitation.id)) },
                        ),
                    ),
                    isOpen = openKey == "invitation-${invitation.id}",
                    onOpenChange = { open ->
                        val key = "invitation-${invitation.id}"
                        if (open) openKey = key else if (openKey == key) openKey = null
                    },
                    onClick = {},
                ) {
                    InvitationRow(invitation, now, languageTag)
                }
            }
        }

        item(key = "actions") { BottomActions(group, state, onEvent) }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun DetailsSection(group: Group, canEdit: Boolean, onEvent: (GroupSettingsEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitleInline(stringResource(Res.string.settings_section_details))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EntityAvatar(
                name = group.name,
                icon = iconFor(IconCategory.Group, group.icon),
                color = EntityColor.fromHex(group.color),
                size = 56.dp,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = group.currencyCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(Res.string.settings_period_summary, group.periodStartDay),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.settings_time_zone_summary, group.timezone),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (group.isArchived) {
            Text(
                text = stringResource(Res.string.group_archived_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canEdit) {
            SecondaryButton(
                text = stringResource(Res.string.action_edit),
                onClick = { onEvent(GroupSettingsEvent.EditClicked) },
            )
        }
    }
}

@Composable
private fun SectionTitleInline(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MemberRow(member: Member, isYou: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntityAvatar(name = member.displayName, icon = null, color = null)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = if (isYou) stringResource(Res.string.member_name_you, member.displayName) else member.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(member.role.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InvitationRow(invitation: Invitation, now: Instant, languageTag: String) {
    val expired = invitation.isExpired(now)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        Text(
            text = invitation.code,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "ss01, zero, tnum", letterSpacing = 1.sp),
            color = if (expired) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.invitation_summary,
                stringResource(invitation.role.labelRes()),
                if (expired) {
                    stringResource(Res.string.invitation_expired)
                } else {
                    stringResource(Res.string.invitation_expires_on, formatDate(invitation.expiresAt, languageTag))
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Owners archive (or reopen) and delete; everyone leaves. Nothing is confirmed: each one is undone from the snackbar. */
@Composable
private fun BottomActions(group: Group, state: GroupSettingsUiState, onEvent: (GroupSettingsEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ErrorText(state.actionError, modifier = Modifier.fillMaxWidth())
        if (group.isOwner) {
            if (state.canReopen) {
                SecondaryButton(
                    text = stringResource(Res.string.action_reopen_group),
                    onClick = { onEvent(GroupSettingsEvent.ReopenClicked) },
                    enabled = !state.isWorking,
                )
            } else if (state.canArchive) {
                SecondaryButton(
                    text = stringResource(Res.string.action_archive_group),
                    onClick = { onEvent(GroupSettingsEvent.ArchiveClicked) },
                    enabled = !state.isWorking,
                )
            }
            DestructiveButton(
                text = stringResource(Res.string.action_delete_group),
                onClick = { onEvent(GroupSettingsEvent.DeleteClicked) },
                enabled = !state.isWorking,
            )
        }
        DestructiveButton(
            text = stringResource(Res.string.action_leave_group),
            onClick = { onEvent(GroupSettingsEvent.LeaveClicked) },
            enabled = !state.isWorking,
        )
    }
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
                invitations = listOf(Invitation("i1", "A1B2C3D4E5F6", GroupRole.Member, Instant.parse("2026-12-01T10:00:00Z"))),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
