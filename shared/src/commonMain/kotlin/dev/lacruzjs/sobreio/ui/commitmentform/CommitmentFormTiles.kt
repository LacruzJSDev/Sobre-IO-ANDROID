// Commitment form: the contribution tiles, the funded-envelope note and the "every N" stepper.

package dev.lacruzjs.sobreio.ui.commitmentform

import dev.lacruzjs.sobreio.ui.components.LabelTooltip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.commitment_funded_bills_note
import dev.lacruzjs.sobreio.generated.resources.commitment_funded_no_bills_note
import dev.lacruzjs.sobreio.generated.resources.commitment_no_groups_note
import dev.lacruzjs.sobreio.generated.resources.every_next
import dev.lacruzjs.sobreio.generated.resources.every_previous
import dev.lacruzjs.sobreio.generated.resources.every_value
import dev.lacruzjs.sobreio.generated.resources.field_dest_account
import dev.lacruzjs.sobreio.generated.resources.field_dest_envelope
import dev.lacruzjs.sobreio.generated.resources.field_dest_group
import dev.lacruzjs.sobreio.generated.resources.field_source_envelope
import dev.lacruzjs.sobreio.generated.resources.unit_days
import dev.lacruzjs.sobreio.generated.resources.unit_months
import dev.lacruzjs.sobreio.generated.resources.unit_weeks
import dev.lacruzjs.sobreio.generated.resources.unit_years
import dev.lacruzjs.sobreio.ui.commitments.labelRes
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.MoneyAmount
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.components.unassignedOption
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/** Why the envelope list only has Bills envelopes while the reserve is on, or what to do when there are none. */
@Composable
internal fun FundedEnvelopeNote(hasBillsEnvelope: Boolean) {
    FormNote(
        stringResource(
            if (hasBillsEnvelope) Res.string.commitment_funded_bills_note else Res.string.commitment_funded_no_bills_note,
        ),
    )
}

/** Destination group, its account, and the optional envelopes on both sides of a contribution. */
@Composable
internal fun ContributionTiles(
    state: CommitmentFormUiState,
    open: OpenTile,
    onToggle: (OpenTile) -> Unit,
    onEvent: (CommitmentFormEvent) -> Unit,
    unassigned: String,
    accountOptions: List<EntityOption>,
    envelopeOptions: List<EntityOption>,
    languageTag: String,
) {
    val creating = !state.isEditing
    // Editing only shows what was chosen: the lists of the other group are not loaded then, so the stored choice is added
    fun List<EntityOption>.withSelected(id: String?, name: String?): List<EntityOption> =
        if (id == null || name == null || any { it.id == id }) this else listOf(EntityOption(id, name)) + this
    val money = { cents: Long -> formatMoney(cents, state.currencyCode, languageTag) }

    if (creating && state.groups.isEmpty()) FormNote(stringResource(Res.string.commitment_no_groups_note))
    EntitySelect(
        label = stringResource(Res.string.field_dest_group),
        options = state.groups.map { it.toEntityOption() }.withSelected(state.destGroupId, state.destGroupName),
        selectedId = state.destGroupId,
        onSelect = { it?.let { id -> onEvent(CommitmentFormEvent.DestGroupSelected(id)) } },
        expanded = open == OpenTile.DestGroup,
        onToggle = { onToggle(OpenTile.DestGroup) },
        enabled = creating && !state.isSubmitting,
        error = state.destGroupError,
    )
    if (state.destGroupId != null) {
        val destAccountOptions = state.destAccounts.map { it.toEntityOption(balance = MoneyAmount(it.balanceCents, state.currencyCode)) }
        EntitySelect(
            label = stringResource(Res.string.field_dest_account),
            options = destAccountOptions,
            selectedId = state.destAccountId,
            onSelect = { it?.let { id -> onEvent(CommitmentFormEvent.DestAccountSelected(id)) } },
            expanded = open == OpenTile.DestAccount,
            onToggle = { onToggle(OpenTile.DestAccount) },
            enabled = creating && !state.isSubmitting,
            error = state.destAccountError,
        )
        if (state.isLoadingDestination) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
    }
    EntitySelect(
        label = stringResource(Res.string.field_source_envelope),
        // With the reserve on, only Bills envelopes: "Unassigned" cannot hold a reserve
        options = (if (state.fundedEffective) emptyList() else listOf(unassignedOption(unassigned))) + envelopeOptions,
        selectedId = state.envelopeId,
        onSelect = { onEvent(CommitmentFormEvent.EnvelopeSelected(it)) },
        expanded = open == OpenTile.Envelope,
        onToggle = { onToggle(OpenTile.Envelope) },
        enabled = creating && !state.isSubmitting,
        error = state.envelopeError,
    )
    if (state.fundedEffective) FundedEnvelopeNote(state.hasBillsEnvelope)
    if (state.destGroupId != null) {
        EntitySelect(
            label = stringResource(Res.string.field_dest_envelope),
            options = listOf(unassignedOption(unassigned)) + state.destEnvelopes.map { it.toEntityOption(balance = MoneyAmount(it.balanceCents, state.currencyCode)) },
            selectedId = state.destEnvelopeId,
            onSelect = { onEvent(CommitmentFormEvent.DestEnvelopeSelected(it)) },
            expanded = open == OpenTile.DestEnvelope,
            onToggle = { onToggle(OpenTile.DestEnvelope) },
            enabled = creating && !state.isSubmitting,
        )
    }
}

internal fun FrequencyUnit.labelRes() = when (this) {
    FrequencyUnit.Day -> Res.string.unit_days
    FrequencyUnit.Week -> Res.string.unit_weeks
    FrequencyUnit.Month -> Res.string.unit_months
    FrequencyUnit.Year -> Res.string.unit_years
}

/** "-  N  +" inside a tile, between 1 and 99. */
@Composable
internal fun EveryStepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LabelTooltip(stringResource(Res.string.every_previous)) {
            IconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > MIN_EVERY_N) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(Res.string.every_previous))
            }
        }
        Text(
            text = stringResource(Res.string.every_value, value),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp),
        )
        LabelTooltip(stringResource(Res.string.every_next)) {
            IconButton(onClick = { onChange(value + 1) }, enabled = enabled && value < MAX_EVERY_N) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.every_next))
            }
        }
    }
}
