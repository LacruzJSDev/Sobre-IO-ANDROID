// Movements: the filter sheets and the custom date range dialog.

package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.intl.Locale
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.domain.model.MovementType
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.entity_all_accounts
import dev.lacruzjs.sobreio.generated.resources.entity_all_envelopes
import dev.lacruzjs.sobreio.generated.resources.entity_all_people
import dev.lacruzjs.sobreio.generated.resources.entity_all_types
import dev.lacruzjs.sobreio.generated.resources.entity_any_date
import dev.lacruzjs.sobreio.generated.resources.envelopes_unassigned
import dev.lacruzjs.sobreio.generated.resources.filter_account
import dev.lacruzjs.sobreio.generated.resources.filter_date_range
import dev.lacruzjs.sobreio.generated.resources.filter_envelope
import dev.lacruzjs.sobreio.generated.resources.filter_person
import dev.lacruzjs.sobreio.generated.resources.filter_tags
import dev.lacruzjs.sobreio.generated.resources.filter_tags_empty
import dev.lacruzjs.sobreio.generated.resources.filter_type
import dev.lacruzjs.sobreio.generated.resources.filter_dates
import dev.lacruzjs.sobreio.ui.accountdetail.labelRes
import dev.lacruzjs.sobreio.ui.components.EntityMultiPickerSheet
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntityPickerSheet
import dev.lacruzjs.sobreio.ui.components.toEntityOption
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

// Sheets -----------------------------------------------------------------------------------------------------------

@Composable
internal fun MovementsSheets(state: MovementsUiState, onEvent: (MovementsEvent) -> Unit) {
    val languageTag = Locale.current.toLanguageTag()
    val dismiss = { onEvent(MovementsEvent.SheetDismissed) }
    val allIcon = Icons.Filled.Apps
    when (state.sheet) {
        MovementsSheet.None -> Unit
        MovementsSheet.Account -> EntityPickerSheet(
            title = stringResource(Res.string.filter_account),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_accounts), allIcon),
            // Live accounts first, then the archived ones, whose movements are still in the ledger
            options = (state.accounts.filter { !it.isArchived } + state.accounts.filter { it.isArchived }).map(Account::toEntityOption),
            selectedId = state.filter.accountId,
            onSelect = { onEvent(MovementsEvent.AccountSelected(it)) },
            onDismiss = dismiss,
        )
        MovementsSheet.Envelope -> {
            val unassigned = stringResource(Res.string.envelopes_unassigned)
            EntityPickerSheet(
                title = stringResource(Res.string.filter_envelope),
                allOption = EntityOption(null, stringResource(Res.string.entity_all_envelopes), allIcon),
                options = (state.envelopes.filter { !it.isArchived } + state.envelopes.filter { it.isArchived }).map {
                    if (it.kind == EnvelopeKind.System) {
                        it.toEntityOption().copy(name = unassigned, icon = Icons.Filled.Inbox, color = null)
                    } else {
                        it.toEntityOption()
                    }
                },
                selectedId = state.filter.envelopeId,
                onSelect = { onEvent(MovementsEvent.EnvelopeSelected(it)) },
                onDismiss = dismiss,
            )
        }
        MovementsSheet.Type -> EntityPickerSheet(
            title = stringResource(Res.string.filter_type),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_types), allIcon),
            options = MovementType.entries.map { EntityOption(it.name, stringResource(it.labelRes()), it.icon()) },
            selectedId = state.filter.type?.name,
            onSelect = { id -> onEvent(MovementsEvent.TypeSelected(id?.let { MovementType.valueOf(it) })) },
            onDismiss = dismiss,
        )
        MovementsSheet.Dates -> EntityPickerSheet(
            title = stringResource(Res.string.filter_dates),
            allOption = EntityOption(null, stringResource(Res.string.entity_any_date), allIcon),
            options = DatePreset.entries
                .filter { it != DatePreset.PreviousPeriod || state.dateContext?.previousPeriod != null }
                .map { preset ->
                    val range = state.dateContext?.let { datePresetRange(preset, it) }
                    EntityOption(
                        id = preset.name,
                        name = stringResource(preset.labelRes()),
                        icon = allIcon,
                        trailing = range?.let { dateRangeText(it.first, it.second, languageTag) },
                    )
                },
            selectedId = state.datePreset?.name,
            onSelect = { id -> onEvent(MovementsEvent.DatePresetSelected(id?.let { DatePreset.valueOf(it) })) },
            onDismiss = dismiss,
        )
        MovementsSheet.Tags -> EntityMultiPickerSheet(
            title = stringResource(Res.string.filter_tags),
            options = state.tags.map { EntityOption(it.id, it.name, color = EntityColor.fromHex(it.color)) },
            selectedIds = state.filter.tagIds,
            onApply = { onEvent(MovementsEvent.TagsApplied(it)) },
            onDismiss = dismiss,
            emptyText = stringResource(Res.string.filter_tags_empty),
        )
        MovementsSheet.Person -> EntityPickerSheet(
            title = stringResource(Res.string.filter_person),
            allOption = EntityOption(null, stringResource(Res.string.entity_all_people), allIcon),
            options = state.members.map { it.toEntityOption() },
            selectedId = state.filter.createdBy,
            onSelect = { onEvent(MovementsEvent.PersonSelected(it)) },
            onDismiss = dismiss,
        )
    }
    if (state.customRangeOpen) {
        CustomRangeDialog(
            from = state.filter.from,
            to = state.filter.to,
            onConfirm = { from, to -> onEvent(MovementsEvent.CustomRangeSelected(from, to)) },
            onDismiss = { onEvent(MovementsEvent.CustomRangeDismissed) },
        )
    }
}

@Composable
private fun dateRangeText(from: LocalDate, to: LocalDate, languageTag: String): String =
    stringResource(Res.string.filter_date_range, formatDayMonth(from, languageTag), formatDayMonth(to, languageTag))
