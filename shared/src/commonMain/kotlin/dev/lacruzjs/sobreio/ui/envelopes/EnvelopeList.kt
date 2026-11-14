package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_assign
import dev.lacruzjs.sobreio.generated.resources.action_delete
import dev.lacruzjs.sobreio.generated.resources.action_edit
import dev.lacruzjs.sobreio.generated.resources.envelope_target_per_month
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_editor
import dev.lacruzjs.sobreio.generated.resources.envelopes_empty_member
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_saving
import dev.lacruzjs.sobreio.generated.resources.envelopes_section_spending
import dev.lacruzjs.sobreio.generated.resources.envelopes_total
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.ic_envelope
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.SwipeAction
import dev.lacruzjs.sobreio.ui.components.SwipeActionsRow
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The summary (total and money still to assign) and the envelopes in two sections, spending and saving. Each row
 * swipes left: Assign for every member, Edit and Delete for owners and admins (one row open at a time). Tapping a row
 * does nothing yet: the envelope detail is a later screen.
 */
@Composable
internal fun EnvelopeList(state: EnvelopesUiState, onEvent: (EnvelopesEvent) -> Unit) {
    // Not saved: a row is always drawn closed when its screen is composed again
    var openEnvelopeId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    // Scrolling the list closes the open row
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openEnvelopeId = null }

    val languageTag = Locale.current.toLanguageTag()
    val assignLabel = stringResource(Res.string.action_assign)
    val editLabel = stringResource(Res.string.action_edit)
    val deleteLabel = stringResource(Res.string.action_delete)

    val section: LazyListScope.(StringResource, List<Envelope>) -> Unit = { title, list ->
        if (list.isNotEmpty()) {
            item(key = "section-${list.first().kind}") {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { envelope ->
                val actions = buildList {
                    add(
                        SwipeAction(
                            label = assignLabel,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { onEvent(EnvelopesEvent.AssignClicked(envelope.id)) },
                        ),
                    )
                    if (state.canEdit) {
                        add(
                            SwipeAction(
                                label = editLabel,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = { onEvent(EnvelopesEvent.EditClicked(envelope.id)) },
                            ),
                        )
                        add(
                            SwipeAction(
                                label = deleteLabel,
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                onClick = { onEvent(EnvelopesEvent.DeleteClicked(envelope.id)) },
                            ),
                        )
                    }
                }
                SwipeActionsRow(
                    actions = actions,
                    isOpen = openEnvelopeId == envelope.id,
                    // A row reporting "closed" must not close another one that has just opened
                    onOpenChange = { open ->
                        if (open) openEnvelopeId = envelope.id else if (openEnvelopeId == envelope.id) openEnvelopeId = null
                    },
                    // The detail screen does not exist yet
                    onClick = {},
                ) {
                    EnvelopeRow(envelope, state.currencyCode, languageTag)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "summary") {
            EnvelopesSummary(state, languageTag)
        }
        if (state.actionError != null) {
            item(key = "action-error") {
                ErrorText(state.actionError, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }
        }
        if (state.envelopes.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(
                        if (state.canEdit) Res.string.envelopes_empty_editor else Res.string.envelopes_empty_member,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 32.dp),
                )
            }
        }
        section(Res.string.envelopes_section_spending, state.spending)
        section(Res.string.envelopes_section_saving, state.saving)
    }
}

/** The total inside the envelopes and, prominent on a soft block, the money still to assign. */
@Composable
private fun EnvelopesSummary(state: EnvelopesUiState, languageTag: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.envelopes_total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMoney(state.totalCents, state.currencyCode, languageTag),
            style = MaterialTheme.typography.headlineMedium,
        )
        Column(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.envelopes_unassigned),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoney(state.unassignedCents, state.currencyCode, languageTag),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun EnvelopeRow(envelope: Envelope, currencyCode: String, languageTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_envelope),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                text = envelope.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (envelope.monthlyTargetCents > 0) {
                Text(
                    text = stringResource(
                        Res.string.envelope_target_per_month,
                        formatMoney(envelope.monthlyTargetCents, currencyCode, languageTag),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = formatMoney(envelope.balanceCents, currencyCode, languageTag),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}
