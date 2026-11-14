package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.MovementType
import kotlinx.datetime.LocalDate

/** The filter chips, in the order they are shown. */
enum class MovementsChip { Account, Envelope, Type, Dates, Tags, Person }

sealed interface MovementsEvent {
    /** The screen is shown (again): pending deletes become final and everything is read again. */
    data object Refresh : MovementsEvent
    data object LoadMore : MovementsEvent
    data object RetryLoadMore : MovementsEvent

    data object SearchOpened : MovementsEvent
    data object SearchClosed : MovementsEvent
    data class SearchChanged(val text: String) : MovementsEvent

    data class ChipClicked(val chip: MovementsChip) : MovementsEvent
    data class ChipCleared(val chip: MovementsChip) : MovementsEvent
    data object ClearAllFilters : MovementsEvent
    data object SheetDismissed : MovementsEvent

    data class AccountSelected(val accountId: String?) : MovementsEvent
    data class EnvelopeSelected(val envelopeId: String?) : MovementsEvent
    data class TypeSelected(val type: MovementType?) : MovementsEvent
    data class DatePresetSelected(val preset: DatePreset?) : MovementsEvent
    data class CustomRangeSelected(val from: LocalDate, val to: LocalDate) : MovementsEvent
    data object CustomRangeDismissed : MovementsEvent
    data class TagsApplied(val tagIds: Set<String>) : MovementsEvent
    data class PersonSelected(val userId: String?) : MovementsEvent

    /** The header "+" and the Edit swipe action only navigate: the Route opens the movement form, the ViewModel ignores them. */
    data object NewClicked : MovementsEvent
    data class EditClicked(val movementId: String, val source: MovementSource) : MovementsEvent

    data class DeleteClicked(val movementId: String) : MovementsEvent
    data object UndoClicked : MovementsEvent
    data object UndoExpired : MovementsEvent
}
