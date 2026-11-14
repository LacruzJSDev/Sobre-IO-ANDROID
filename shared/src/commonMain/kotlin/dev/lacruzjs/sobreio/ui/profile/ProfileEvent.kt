package dev.lacruzjs.sobreio.ui.profile

sealed interface ProfileEvent {
    /** Handled by the Route, which owns the back stack. */
    data object BackClicked : ProfileEvent

    data class NameChanged(val value: String) : ProfileEvent
    data object SaveClicked : ProfileEvent
}
