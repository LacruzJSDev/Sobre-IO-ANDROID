// Group settings: the details, tags, members and invitations sections.

package dev.lacruzjs.sobreio.ui.groupsettings
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.ui.components.TagDot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import dev.lacruzjs.sobreio.generated.resources.field_period_start_short
import dev.lacruzjs.sobreio.generated.resources.field_time_zone
import dev.lacruzjs.sobreio.generated.resources.period_day_value
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.FormTileValue
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.group_archived_banner
import dev.lacruzjs.sobreio.generated.resources.invitation_expires_on
import dev.lacruzjs.sobreio.generated.resources.invitation_summary
import dev.lacruzjs.sobreio.generated.resources.invitation_accepted_on
import dev.lacruzjs.sobreio.generated.resources.settings_section_tags
import dev.lacruzjs.sobreio.generated.resources.settings_tags_title_count
import dev.lacruzjs.sobreio.generated.resources.settings_tags_empty
import dev.lacruzjs.sobreio.generated.resources.settings_tags_more
import dev.lacruzjs.sobreio.generated.resources.action_new_tag
import dev.lacruzjs.sobreio.generated.resources.action_manage_tags
import dev.lacruzjs.sobreio.generated.resources.member_name_you
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.stringResource

/**
 * The tags themselves are the content: the title carries the count and the action (a pencil, or a "+" when there are
 * none yet, like the Invitations "+"), and a soft card previews the tags as coloured chips. Tapping the card or the
 * pencil opens the Tags screen; the "+" opens it with the create sheet. Nothing shows until the tags load.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(tags: List<Tag>?, canManage: Boolean, onEvent: (GroupSettingsEvent) -> Unit) {
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
internal fun DetailsSection(group: Group) {
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
internal fun MemberRow(member: Member, isYou: Boolean) {
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
internal fun PendingInvitationRow(invitation: Invitation, languageTag: String) {
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
internal fun AcceptedInvitationRow(invitation: Invitation, name: String, languageTag: String) {
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
