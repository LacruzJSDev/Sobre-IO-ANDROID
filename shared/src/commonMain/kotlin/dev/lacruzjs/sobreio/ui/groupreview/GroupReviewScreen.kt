package dev.lacruzjs.sobreio.ui.groupreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.action_skip
import dev.lacruzjs.sobreio.generated.resources.group_review_back
import dev.lacruzjs.sobreio.generated.resources.group_review_empty
import dev.lacruzjs.sobreio.generated.resources.group_review_title
import dev.lacruzjs.sobreio.generated.resources.group_summary_due_subtitle
import dev.lacruzjs.sobreio.ui.accountdetail.SoftTile
import dev.lacruzjs.sobreio.ui.commitments.icon
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.EntityAvatar
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.ScreenHeader
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Level 2 of the Group tab: the due dates waiting for review. [onBack] returns to the group, which reads its figures again
 * when it is shown. Reads again each time it is shown.
 */
@Composable
fun GroupReviewRoute(
    groupId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupReviewViewModel = koinViewModel { parametersOf(groupId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onEvent(GroupReviewEvent.Refresh) }
    GroupReviewScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                GroupReviewEvent.BackClicked -> {
                    viewModel.onEvent(event)
                    onBack()
                }
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun GroupReviewScreen(
    state: GroupReviewUiState,
    onEvent: (GroupReviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.group_review_title),
                subtitle = state.groupName.takeIf { it.isNotEmpty() },
                onBack = { onEvent(GroupReviewEvent.BackClicked) },
            )
            Column(modifier = Modifier.weight(1f).navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                ) {
                    when {
                        state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                            TextButton(onClick = { onEvent(GroupReviewEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                        }
                        state.pending.isEmpty() -> EmptyState(message = stringResource(Res.string.group_review_empty)) {
                            ErrorText(state.actionError)
                            PrimaryButton(
                                text = stringResource(Res.string.group_review_back),
                                onClick = { onEvent(GroupReviewEvent.BackClicked) },
                            )
                        }
                        else -> ReviewList(state, onEvent)
                    }
                }
                UndoSnackbarHost(
                    message = state.undoMessage,
                    onUndo = { onEvent(GroupReviewEvent.UndoClicked) },
                    onDismiss = { onEvent(GroupReviewEvent.UndoExpired) },
                )
            }
        }
    }
    ConfirmSheet(state, onEvent)
}

@Composable
private fun ReviewList(state: GroupReviewUiState, onEvent: (GroupReviewEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp))
        state.pending.forEach { item ->
            SoftTile {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // No entity colour: a commitment is drawn in ink with the icon of its kind
                    EntityAvatar(name = item.name, icon = item.kind.icon(), color = null, size = 36.dp)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = stringResource(
                                Res.string.group_summary_due_subtitle,
                                state.accountName(item.accountId),
                                formatDate(item.dueDate, languageTag),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MoneyText(
                        amountCents = item.signedCents,
                        currencyCode = state.currencyCode,
                        role = MoneyRole.Flow,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                if (state.canRecord) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(
                            text = stringResource(Res.string.action_skip),
                            onClick = { onEvent(GroupReviewEvent.SkipClicked(item.key)) },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                        PrimaryButton(
                            text = stringResource(Res.string.action_confirm),
                            onClick = { onEvent(GroupReviewEvent.ConfirmClicked(item.key)) },
                            modifier = Modifier.weight(1f),
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}
