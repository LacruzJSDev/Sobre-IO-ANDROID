package dev.lacruzjs.sobreio.ui.export

sealed interface ExportEvent {
    /** The export icon of the Movements header: opens the sheet and reads the periods. */
    data object Opened : ExportEvent
    data object Dismissed : ExportEvent
    data class PeriodSelected(val periodId: String) : ExportEvent
    data object ExportClicked : ExportEvent
    /** Sent by the Route once the save dialog has answered. */
    data class FileHandled(val result: SaveResult) : ExportEvent
}
