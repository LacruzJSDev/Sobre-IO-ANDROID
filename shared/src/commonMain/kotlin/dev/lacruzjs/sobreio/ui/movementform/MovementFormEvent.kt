package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.model.EntryKind
import kotlinx.datetime.LocalDate

sealed interface MovementFormEvent {
    data object CloseClicked : MovementFormEvent
    data class KindSelected(val kind: EntryKind) : MovementFormEvent
    data class AmountChanged(val value: String) : MovementFormEvent
    /** The account of an expense or income, and the source ("From") of a transfer. */
    data class AccountSelected(val id: String?) : MovementFormEvent
    data class EnvelopeSelected(val id: String?) : MovementFormEvent
    /** The optional envelope that also covers what the chosen one lacks; null is "None". */
    data class CoverSelected(val id: String?) : MovementFormEvent
    data class ToAccountSelected(val id: String?) : MovementFormEvent
    data class CrossGroupChanged(val value: Boolean) : MovementFormEvent
    data class DestGroupSelected(val id: String?) : MovementFormEvent
    data class DestAccountSelected(val id: String?) : MovementFormEvent
    data class DateChanged(val date: LocalDate) : MovementFormEvent
    data class NotesChanged(val value: String) : MovementFormEvent
    data class TagToggled(val id: String) : MovementFormEvent
    /** The search text of the tag picker. */
    data class TagQueryChanged(val value: String) : MovementFormEvent
    /** The picker's "Create" row: makes a tag named like the search text and selects it. */
    data object TagCreateClicked : MovementFormEvent
    data object SaveClicked : MovementFormEvent
    data object Retry : MovementFormEvent
    data object NavigationHandled : MovementFormEvent
}
