package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_archive
import dev.lacruzjs.sobreio.generated.resources.action_assign
import dev.lacruzjs.sobreio.generated.resources.action_cancel_deletion
import dev.lacruzjs.sobreio.generated.resources.action_delete
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.action_leave
import dev.lacruzjs.sobreio.generated.resources.action_remove
import dev.lacruzjs.sobreio.generated.resources.action_reopen
import dev.lacruzjs.sobreio.generated.resources.action_revoke
import dev.lacruzjs.sobreio.generated.resources.action_role
import dev.lacruzjs.sobreio.ui.theme.leaveAction
import dev.lacruzjs.sobreio.ui.theme.onLeaveAction
import org.jetbrains.compose.resources.stringResource

// The one place that maps an action to its icon and colour. Every list uses these, so the same action never looks
// different. Red only for what cannot be taken back; ink for the rest; muted orange for leaving; soft grey for assigning.

@Composable
private fun inkAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) = SwipeAction(
    label = label,
    icon = icon,
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    onClick = onClick,
)

@Composable
private fun redAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) = SwipeAction(
    label = label,
    icon = icon,
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError,
    onClick = onClick,
)

/** Pencil, ink. Opens the edit screen. */
@Composable
fun editAction(onClick: () -> Unit) = inkAction(stringResource(Res.string.action_edit), Icons.Filled.Edit, onClick)

/** Archive box, ink. */
@Composable
fun archiveAction(onClick: () -> Unit) = inkAction(stringResource(Res.string.action_archive), Icons.Filled.Archive, onClick)

/** Box with an up arrow, ink: reopen or restore. */
@Composable
fun reopenAction(onClick: () -> Unit) = inkAction(stringResource(Res.string.action_reopen), Icons.Filled.Unarchive, onClick)

/** Bin with a back arrow, ink: cancel a requested deletion. */
@Composable
fun cancelDeletionAction(onClick: () -> Unit) =
    inkAction(stringResource(Res.string.action_cancel_deletion), Icons.Filled.RestoreFromTrash, onClick)

/** Person with a shield, ink: change a member's role. */
@Composable
fun changeRoleAction(onClick: () -> Unit) = inkAction(stringResource(Res.string.action_role), Icons.Filled.AdminPanelSettings, onClick)

/** Arrow into an envelope, soft grey with an ink icon: assign money. */
@Composable
fun assignAction(onClick: () -> Unit) = SwipeAction(
    label = stringResource(Res.string.action_assign),
    icon = Icons.Filled.MoveToInbox,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurface,
    onClick = onClick,
)

/** Door with an arrow, muted orange (the theme's leave colour). */
@Composable
fun leaveAction(onClick: () -> Unit) = SwipeAction(
    label = stringResource(Res.string.action_leave),
    icon = Icons.AutoMirrored.Filled.Logout,
    containerColor = MaterialTheme.colorScheme.leaveAction,
    contentColor = MaterialTheme.colorScheme.onLeaveAction,
    onClick = onClick,
)

/** Bin, red. */
@Composable
fun deleteAction(onClick: () -> Unit) = redAction(stringResource(Res.string.action_delete), Icons.Filled.Delete, onClick)

/** Person with a minus, red: remove a member from the group. */
@Composable
fun removeMemberAction(onClick: () -> Unit) = redAction(stringResource(Res.string.action_remove), Icons.Filled.PersonRemove, onClick)

/** Broken link, red: revoke an invitation. */
@Composable
fun revokeAction(onClick: () -> Unit) = redAction(stringResource(Res.string.action_revoke), Icons.Filled.LinkOff, onClick)
