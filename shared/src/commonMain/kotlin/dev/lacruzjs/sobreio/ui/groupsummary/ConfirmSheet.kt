package dev.lacruzjs.sobreio.ui.groupsummary

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.generated.resources.field_amount
import dev.lacruzjs.sobreio.generated.resources.field_cover_also
import dev.lacruzjs.sobreio.generated.resources.field_date
import dev.lacruzjs.sobreio.generated.resources.group_summary_confirm_title
import dev.lacruzjs.sobreio.generated.resources.movement_cover_none
import dev.lacruzjs.sobreio.generated.resources.movement_cover_note
import dev.lacruzjs.sobreio.ui.components.AmountField
import dev.lacruzjs.sobreio.ui.components.DateFormTile
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.MoneyAmount
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/**
 * One-step sheet that registers a due date: the amount and date start as expected, and an expense whose envelope cannot
 * cover it shows the same cover note and optional "Also cover from" envelope as the movement form.
 */
@Composable
internal fun ConfirmSheet(state: GroupSummaryUiState, onEvent: (GroupSummaryEvent) -> Unit) {
    val draft = state.sheet ?: return
    val languageTag = Locale.current.toLanguageTag()
    val busy = draft.isSubmitting
    FlatBottomSheet(
        title = stringResource(Res.string.group_summary_confirm_title, draft.occurrence.name),
        onDismiss = { onEvent(GroupSummaryEvent.SheetDismissed) },
    ) {
        AmountField(
            value = draft.amount,
            onValueChange = { onEvent(GroupSummaryEvent.AmountChanged(it)) },
            label = stringResource(Res.string.field_amount),
            errorRes = draft.amountError,
            enabled = !busy,
        )
        DateFormTile(
            label = stringResource(Res.string.field_date),
            date = draft.date,
            onDateSelected = { onEvent(GroupSummaryEvent.DateChanged(it)) },
            enabled = !busy,
            maxDate = state.today,
        )
        val short = state.coverShortEnvelope(draft)
        if (short != null) {
            FormNote(
                stringResource(
                    Res.string.movement_cover_note,
                    short.name,
                    formatMoney(short.balanceCents, state.currencyCode, languageTag),
                ),
            )
            val candidates = state.coverCandidates(draft)
            if (candidates.isNotEmpty()) {
                var open by rememberSaveable { mutableStateOf(false) }
                EntitySelect(
                    label = stringResource(Res.string.field_cover_also),
                    options = listOf(EntityOption(null, stringResource(Res.string.movement_cover_none))) +
                        candidates.map { it.toEntityOption(balance = MoneyAmount(it.balanceCents, state.currencyCode)) },
                    selectedId = draft.coverEnvelopeId,
                    onSelect = { onEvent(GroupSummaryEvent.CoverSelected(it)) },
                    expanded = open,
                    onToggle = { open = !open },
                    enabled = !busy,
                )
            }
        }
        ErrorText(draft.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_confirm),
            onClick = { onEvent(GroupSummaryEvent.ConfirmSubmitted) },
            isLoading = busy,
        )
    }
}
