package dev.lacruzjs.sobreio.ui.profile

import dev.lacruzjs.sobreio.ui.export.SaveResult

sealed interface ProfileEvent {
    /** Handled by the Route, which owns the back stack. */
    data object BackClicked : ProfileEvent

    /** Handled by the Route: opens the delete account screen. */
    data object DeleteAccountClicked : ProfileEvent

    data class NameChanged(val value: String) : ProfileEvent
    data object SaveClicked : ProfileEvent
    data class ColorBlindSafeChanged(val enabled: Boolean) : ProfileEvent

    /** Handled by the Route: opens a legal document in the browser. */
    data class OpenDocument(val url: String) : ProfileEvent

    data object DownloadDataClicked : ProfileEvent

    /** The save dialog for the data closed. */
    data class DataFileHandled(val result: SaveResult) : ProfileEvent
}
