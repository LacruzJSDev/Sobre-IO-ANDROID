package dev.lacruzjs.sobreio.ui.envelopetransfers

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer
import dev.lacruzjs.sobreio.domain.model.TransferOrigin
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_section_transfers
import dev.lacruzjs.sobreio.generated.resources.envelope_detail_transfers_empty
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_new_title
import dev.lacruzjs.sobreio.generated.resources.envelope_transfer_unknown_envelope
import dev.lacruzjs.sobreio.generated.resources.envelope_transfers_route
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.row_subtitle
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconCategory
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.components.contentMaxWidth
import dev.lacruzjs.sobreio.ui.components.deleteAction
import dev.lacruzjs.sobreio.ui.components.editAction
import dev.lacruzjs.sobreio.ui.components.iconFor
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import dev.lacruzjs.sobreio.ui.undo.CommitUndoOnBack
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 2 of the Envelopes tab: every move between envelopes of the group. Holds the back stack lambdas: [onNew] and
 * [onEdit] open the move form. It reads again each time it is shown, so coming back from the form shows the change.
 */
@Composable
fun EnvelopeTransfersRoute(
    groupId: String,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (transferId: String) -> Unit,
    modifier: Modifier = Modifier,
    /** Limits the list to one envelope's moves (opened from its detail); null lists the whole group. */
    envelopeId: String? = null,
    viewModel: EnvelopeTransfersViewModel = koinViewModel { parametersOf(groupId, envelopeId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(EnvelopeTransfersEvent.Refresh) }
    val leave = {
        viewModel.onEvent(EnvelopeTransfersEvent.BackClicked)
        onBack()
    }
    CommitUndoOnBack(pending = state.undoMessage != null, onLeave = leave)
    EnvelopeTransfersScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                EnvelopeTransfersEvent.BackClicked -> leave()
                EnvelopeTransfersEvent.NewClicked -> onNew()
                is EnvelopeTransfersEvent.EditClicked -> onEdit(event.transferId)
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

/**
 * The moves between envelopes, newest first: the destination's avatar, "From → To", what made it and the day, and the
 * amount. Members of a live group swipe Edit (manual moves and targets) and Delete (also reserves), with Undo; "+"
 * records a new manual move.
 */
@Composable
fun EnvelopeTransfersScreen(
    state: EnvelopeTransfersUiState,
    onEvent: (EnvelopeTransfersEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val languageTag = Locale.current.toLanguageTag()
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().contentMaxWidth()) {
            ScreenHeader(
                title = stringResource(Res.string.envelope_detail_section_transfers),
                // One envelope's moves name the envelope; the whole group's name the group
                subtitle = when {
                    state.envelopeIsUnassigned -> stringResource(Res.string.envelopes_unassigned)
                    state.envelopeName != null -> state.envelopeName
                    else -> state.groupName.takeIf { it.isNotEmpty() }
                },
                onBack = { onEvent(EnvelopeTransfersEvent.BackClicked) },
                actions = {
                    if (state.canWrite) {
                        AddIconButton(
                            contentDescription = stringResource(Res.string.envelope_transfer_new_title),
                            onClick = { onEvent(EnvelopeTransfersEvent.NewClicked) },
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
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                        TextButton(onClick = { onEvent(EnvelopeTransfersEvent.Retry) }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                    state.rows.isEmpty() && state.actionError == null ->
                        EmptyState(message = stringResource(Res.string.envelope_detail_transfers_empty))
                    else -> TransferList(state, languageTag, onEvent)
                }
            }
            // The amount of a deleted move is written here, where the language and the currency are known
            val undoMessage = state.undoMessage?.let { message ->
                message.amountCents
                    ?.let { cents -> message.copy(argument = formatMoney(cents, state.currencyCode, languageTag)) }
                    ?: message
            }
            UndoSnackbarHost(
                message = undoMessage,
                onUndo = { onEvent(EnvelopeTransfersEvent.UndoClicked) },
                onDismiss = { onEvent(EnvelopeTransfersEvent.UndoExpired) },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun TransferList(state: EnvelopeTransfersUiState, languageTag: String, onEvent: (EnvelopeTransfersEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openId = null }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        items(state.rows, key = { it.transfer.id }) { row ->
            // Near the end, ask for the next page
            if (state.hasMore && row === state.rows[maxOf(0, state.rows.size - LOAD_MORE_AHEAD)]) {
                LaunchedEffect(row.transfer.id) { onEvent(EnvelopeTransfersEvent.LoadMore) }
            }
            val actions = if (state.canWrite) {
                listOfNotNull(
                    if (row.canEdit) editAction { onEvent(EnvelopeTransfersEvent.EditClicked(row.transfer.id)) } else null,
                    if (row.canDelete) deleteAction { onEvent(EnvelopeTransfersEvent.DeleteClicked(row.transfer.id)) } else null,
                )
            } else {
                emptyList()
            }
            if (actions.isEmpty()) {
                TransferRowItem(row, state.currencyCode, languageTag)
            } else {
                SwipeActionsRow(
                    actions = actions,
                    isOpen = openId == row.transfer.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openId = row.transfer.id else if (openId == row.transfer.id) openId = null
                    },
                    onClick = { if (row.canEdit) onEvent(EnvelopeTransfersEvent.EditClicked(row.transfer.id)) },
                ) {
                    TransferRowItem(row, state.currencyCode, languageTag)
                }
            }
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        if (state.loadMoreFailed) {
            item(key = "load-more-failed") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { onEvent(EnvelopeTransfersEvent.LoadMore) }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferRowItem(row: GroupTransferRow, currencyCode: String, languageTag: String) {
    val from = envelopeName(row.from)
    val to = envelopeName(row.to)
    val date = formatDate(row.date, languageTag)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val destination = row.to
        EntityAvatar(
            name = to,
            icon = if (destination?.kind == EnvelopeKind.System) Icons.Filled.Inbox else iconFor(IconCategory.Envelope, destination?.icon),
            color = EntityColor.fromHex(destination?.color),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.envelope_transfers_route, from, to),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.row_subtitle, row.transfer.originLabel(), date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MoneyText(
            amountCents = row.transfer.amountCents,
            currencyCode = currencyCode,
            role = MoneyRole.Transfer,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** The system envelope is stored with a Spanish name: the UI has its own, translated. */
@Composable
private fun envelopeName(envelope: Envelope?): String = when {
    envelope == null -> stringResource(Res.string.envelope_transfer_unknown_envelope)
    envelope.kind == EnvelopeKind.System -> stringResource(Res.string.envelopes_unassigned)
    else -> envelope.name
}

/** How many rows before the end the next page is asked for. */
private const val LOAD_MORE_AHEAD = 10

@PreviewLightDark
@Composable
private fun EnvelopeTransfersScreenPreview() {
    val unassigned = Envelope("u", "g", "Sin asignar", EnvelopeKind.System, 0, 0)
    val food = Envelope("e1", "g", "Comida", EnvelopeKind.Spending, 40_000, 12_000, color = "#82B1D1", icon = "cart")
    SobreioTheme {
        EnvelopeTransfersScreen(
            state = EnvelopeTransfersUiState(
                isLoading = false,
                groupName = "Casa",
                canWrite = true,
                rows = listOf(
                    GroupTransferRow(
                        EnvelopeTransfer("t1", "u", "e1", 40_000, TransferOrigin.Target, Instant.parse("2026-11-01T08:00:00Z")),
                        unassigned,
                        food,
                        LocalDate(2026, 11, 1),
                    ),
                ),
                total = 1,
            ),
            onEvent = {},
        )
    }
}
