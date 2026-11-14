package dev.lacruzjs.sobreio.ui.groupform

import dev.lacruzjs.sobreio.ui.groups.GroupFormState

sealed interface GroupFormEvent {
    /** Handled by the Route, which owns the back stack: close without saving, and open the time zone picker. */
    data object CloseClicked : GroupFormEvent
    data object TimeZoneClicked : GroupFormEvent

    /** The picker returned a time zone id (the Route passes it on). */
    data class TimeZonePicked(val id: String) : GroupFormEvent

    /** Any change of the fields: name, currency, colour, icon or period start day. */
    data class FormChanged(val form: GroupFormState) : GroupFormEvent
    data object SaveClicked : GroupFormEvent
    data object Retry : GroupFormEvent
    data object NavigationHandled : GroupFormEvent
}
