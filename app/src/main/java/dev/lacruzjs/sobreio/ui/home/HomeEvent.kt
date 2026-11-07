package dev.lacruzjs.sobreio.ui.home

sealed interface HomeEvent {
    data object SignOutClicked : HomeEvent
}
