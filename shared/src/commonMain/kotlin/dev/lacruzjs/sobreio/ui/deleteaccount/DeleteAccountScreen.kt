package dev.lacruzjs.sobreio.ui.deleteaccount

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.delete_account_action
import dev.lacruzjs.sobreio.generated.resources.delete_account_blocked_note
import dev.lacruzjs.sobreio.generated.resources.delete_account_blocked_title
import dev.lacruzjs.sobreio.generated.resources.delete_account_confirm_label
import dev.lacruzjs.sobreio.generated.resources.delete_account_delete_note
import dev.lacruzjs.sobreio.generated.resources.delete_account_delete_title
import dev.lacruzjs.sobreio.generated.resources.delete_account_final_note
import dev.lacruzjs.sobreio.generated.resources.delete_account_intro
import dev.lacruzjs.sobreio.generated.resources.delete_account_leave_note
import dev.lacruzjs.sobreio.generated.resources.delete_account_leave_title
import dev.lacruzjs.sobreio.generated.resources.delete_account_no_groups
import dev.lacruzjs.sobreio.generated.resources.delete_account_title
import dev.lacruzjs.sobreio.ui.components.DestructiveButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormSection
import dev.lacruzjs.sobreio.ui.components.FormTile
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeleteAccountRoute(
    onBack: () -> Unit,
    onOpenGroupSettings: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Back from a group's settings the roles may have changed
    LaunchedEffect(Unit) { viewModel.onEvent(DeleteAccountEvent.Refresh) }
    DeleteAccountScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                DeleteAccountEvent.BackClicked -> onBack()
                is DeleteAccountEvent.BlockedGroupClicked -> onOpenGroupSettings(event.groupId)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * What deleting the account does to each group (blocked first, then deleted for good, then left), the warning that it
 * cannot be undone, the "I understand" switch and the red button. The switch is the confirmation: no dialog.
 */
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountUiState,
    onEvent: (DeleteAccountEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.delete_account_title),
                onBack = { onEvent(DeleteAccountEvent.BackClicked) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.items == null -> EmptyState(
                        message = stringResource((state.loadError ?: return@Box).messageRes()),
                    ) {
                        TextButton(onClick = { onEvent(DeleteAccountEvent.Retry) }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                    else -> Content(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun Content(state: DeleteAccountUiState, onEvent: (DeleteAccountEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.delete_account_intro),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (state.items.orEmpty().isEmpty()) {
            FormNote(stringResource(Res.string.delete_account_no_groups))
        }
        GroupSection(
            title = stringResource(Res.string.delete_account_blocked_title),
            note = stringResource(Res.string.delete_account_blocked_note),
            items = state.blocked,
            onClick = { onEvent(DeleteAccountEvent.BlockedGroupClicked(it.groupId)) },
            emphasised = true,
        )
        GroupSection(
            title = stringResource(Res.string.delete_account_delete_title),
            note = stringResource(Res.string.delete_account_delete_note),
            items = state.deleted,
        )
        GroupSection(
            title = stringResource(Res.string.delete_account_leave_title),
            note = stringResource(Res.string.delete_account_leave_note),
            items = state.left,
        )
        FormNote(stringResource(Res.string.delete_account_final_note))
        FormTile(label = stringResource(Res.string.delete_account_confirm_label)) {
            Switch(
                checked = state.confirmed,
                onCheckedChange = { onEvent(DeleteAccountEvent.ConfirmChanged(it)) },
                enabled = !state.isDeleting,
            )
        }
        ErrorText(state.deleteError, modifier = Modifier.fillMaxWidth())
        DestructiveButton(
            text = stringResource(Res.string.delete_account_action),
            onClick = { onEvent(DeleteAccountEvent.DeleteClicked) },
            isLoading = state.isDeleting,
            enabled = state.canDelete,
        )
    }
}

/** Hidden when [items] is empty. Rows open [onClick] when given; [emphasised] colours the title as a warning. */
@Composable
private fun GroupSection(
    title: String,
    note: String,
    items: List<DeletionPreviewItem>,
    modifier: Modifier = Modifier,
    onClick: ((DeletionPreviewItem) -> Unit)? = null,
    emphasised: Boolean = false,
) {
    if (items.isEmpty()) return
    FormSection(title = title, modifier = modifier) {
        if (emphasised) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            FormNote(note)
        }
        items.forEach { item -> GroupRow(item, onClick?.let { { it(item) } }) }
    }
}

@Composable
private fun GroupRow(item: DeletionPreviewItem, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EntityAvatar(
            name = item.name,
            icon = iconFor(IconCategory.Group, item.icon),
            color = EntityColor.fromHex(item.color),
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun DeleteAccountScreenPreview() {
    SobreioTheme {
        DeleteAccountScreen(
            state = DeleteAccountUiState(
                isLoading = false,
                items = listOf(
                    DeletionPreviewItem("1", "Casa", "#82B1D1", "home", DeletionOutcome.Delete),
                    DeletionPreviewItem("2", "Viaje", null, null, DeletionOutcome.Leave),
                ),
                confirmed = true,
            ),
            onEvent = {},
        )
    }
}
