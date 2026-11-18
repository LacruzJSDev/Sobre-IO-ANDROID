package dev.lacruzjs.sobreio.ui.groupsettings
import dev.lacruzjs.sobreio.ui.components.DetailSkeleton
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.ui.components.TagDot
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import dev.lacruzjs.sobreio.generated.resources.edit_group_title
import dev.lacruzjs.sobreio.generated.resources.field_period_start_short
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.period_day_value
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_invite
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.group_archived_banner
import dev.lacruzjs.sobreio.generated.resources.group_settings_title
import dev.lacruzjs.sobreio.generated.resources.invitation_expires_on
import dev.lacruzjs.sobreio.generated.resources.invitation_summary
import dev.lacruzjs.sobreio.generated.resources.invitation_accepted_on
import dev.lacruzjs.sobreio.generated.resources.invitations_empty
import dev.lacruzjs.sobreio.generated.resources.member_former
import dev.lacruzjs.sobreio.generated.resources.settings_section_accepted
import dev.lacruzjs.sobreio.generated.resources.settings_section_pending
import dev.lacruzjs.sobreio.generated.resources.settings_section_tags
import dev.lacruzjs.sobreio.generated.resources.settings_tags_title_count
import dev.lacruzjs.sobreio.generated.resources.settings_tags_empty
import dev.lacruzjs.sobreio.generated.resources.settings_tags_more
import dev.lacruzjs.sobreio.generated.resources.action_new_tag
import dev.lacruzjs.sobreio.generated.resources.action_manage_tags
import dev.lacruzjs.sobreio.generated.resources.member_name_you
import dev.lacruzjs.sobreio.generated.resources.settings_section_invitations
import dev.lacruzjs.sobreio.generated.resources.settings_section_members
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.changeRoleAction
import dev.lacruzjs.sobreio.ui.components.removeMemberAction
import dev.lacruzjs.sobreio.ui.components.revokeAction
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.EntityColor
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
        Column(modifier = Modifier.fillMaxSize().contentMaxWidth()) {
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
    // Not saved: a row is always drawn closed when its screen is composed again
    var openKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openKey = null }

    val languageTag = Locale.current.toLanguageTag()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "details") { DetailsSection(group) }

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

        item(key = "tags") { TagsSection(state.tags, state.canManageTags, onEvent) }

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
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 4.dp),
    )
}

/**
 * The tags themselves are the content: the title carries the count and the action (a pencil, or a "+" when there are
 * none yet, like the Invitations "+"), and a soft card previews the tags as coloured chips. Tapping the card or the
 * pencil opens the Tags screen; the "+" opens it with the create sheet. Nothing shows until the tags load.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<Tag>?, canManage: Boolean, onEvent: (GroupSettingsEvent) -> Unit) {
    if (tags == null) return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = stringResource(Res.string.settings_section_tags)
            SectionTitle(
                text = if (tags.isEmpty()) title else stringResource(Res.string.settings_tags_title_count, title, tags.size),
                modifier = Modifier.weight(1f),
            )
            if (tags.isEmpty()) {
                if (canManage) {
                    AddIconButton(
                        contentDescription = stringResource(Res.string.action_new_tag),
                        onClick = { onEvent(GroupSettingsEvent.NewTagClicked) },
                    )
                }
            } else {
                IconActionButton(
                    icon = Icons.Outlined.Edit,
                    // Same visual weight as the thin "+" of the other section titles
                    iconSize = 24.dp,
                    label = stringResource(Res.string.action_manage_tags),
                    onClick = { onEvent(GroupSettingsEvent.TagsClicked) },
                )
            }
        }
        if (tags.isEmpty()) {
            Text(
                text = stringResource(Res.string.settings_tags_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        } else {
            val shown = tags.take(MAX_TAG_PREVIEW)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button) { onEvent(GroupSettingsEvent.TagsClicked) }
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shown.forEach { tag -> TagPreviewChip(tag.name, tag.color) }
                if (tags.size > shown.size) {
                    TagPreviewChip(stringResource(Res.string.settings_tags_more, tags.size - shown.size), color = null, muted = true)
                }
            }
        }
    }
}

/** Up to this many tags are previewed; the rest are summed up in a "+N more" chip. */
private const val MAX_TAG_PREVIEW = 8

@Composable
private fun TagPreviewChip(text: String, color: String?, muted: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!muted) TagDot(color, size = 9.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailsSection(group: Group) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EntityAvatar(
                name = group.name,
                icon = iconFor(IconCategory.Group, group.icon),
                color = EntityColor.fromHex(group.color),
                size = 64.dp,
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (group.isArchived) {
                        "${group.currencyCode} · ${stringResource(Res.string.group_archived_banner)}"
                    } else {
                        group.currencyCode
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormTile(label = stringResource(Res.string.field_period_start_short)) {
                FormTileValue(stringResource(Res.string.period_day_value, group.periodStartDay))
            }
            FormTile(label = stringResource(Res.string.field_time_zone)) { FormTileValue(group.timezone) }
        }
    }
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
private fun PendingInvitationRow(invitation: Invitation, languageTag: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Tapping the row copies the code
            .clickable(role = Role.Button) { clipboard.setText(AnnotatedString(invitation.code)) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            text = invitation.code,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "ss01, zero, tnum", letterSpacing = 1.sp),
        )
        Text(
            text = stringResource(
                Res.string.invitation_summary,
                stringResource(invitation.role.labelRes()),
                stringResource(Res.string.invitation_expires_on, formatDate(invitation.expiresAt, languageTag)),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AcceptedInvitationRow(invitation: Invitation, name: String, languageTag: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = stringResource(
                Res.string.invitation_summary,
                stringResource(invitation.role.labelRes()),
                invitation.acceptedAt?.let { stringResource(Res.string.invitation_accepted_on, formatDate(it, languageTag)) }
                    .orEmpty(),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                pendingInvitations = listOf(Invitation("i1", "A1B2C3D4E5F6", GroupRole.Member, Instant.parse("2026-12-01T10:00:00Z"))),
                isLoading = false,
            ),
            onEvent = {},
        )
    }
}
