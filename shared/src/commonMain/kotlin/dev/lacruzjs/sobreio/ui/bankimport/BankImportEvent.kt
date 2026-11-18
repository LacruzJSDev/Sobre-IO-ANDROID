package dev.lacruzjs.sobreio.ui.bankimport

import dev.lacruzjs.sobreio.domain.bankimport.AmountMode
import dev.lacruzjs.sobreio.domain.bankimport.DateOrder
import dev.lacruzjs.sobreio.domain.bankimport.DecimalMark
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.ui.bankimport.picker.PickResult

sealed interface BankImportEvent {
    data object Retry : BankImportEvent

    /** Handled by the Route, which opens the system picker. */
    data object ChooseFileClicked : BankImportEvent
    data class FilePicked(val result: PickResult) : BankImportEvent

    // Columns
    data class ColumnTileToggled(val tile: ColumnTile) : BankImportEvent
    data class ColumnChosen(val tile: ColumnTile, val column: Int) : BankImportEvent
    data class AmountModeChosen(val mode: AmountMode) : BankImportEvent
    data class DateOrderChosen(val order: DateOrder) : BankImportEvent
    data class DecimalMarkChosen(val mark: DecimalMark) : BankImportEvent
    data object ColumnsContinueClicked : BankImportEvent
    data object EditColumnsClicked : BankImportEvent

    // Review
    data class RowClicked(val index: Int) : BankImportEvent
    data class RowIgnoreSwiped(val index: Int) : BankImportEvent
    data object UndoClicked : BankImportEvent
    data object UndoExpired : BankImportEvent
    /** [tagName] is the import tag in the user's language, which only the screen knows. */
    data class ImportClicked(val tagName: String) : BankImportEvent

    // Row sheet
    data object SheetDismissed : BankImportEvent
    data class SheetActionChosen(val action: BankRowAction) : BankImportEvent
    data class SheetTransferChosen(val isTransfer: Boolean) : BankImportEvent
    data class SheetSelectToggled(val select: RowSelect) : BankImportEvent
    data class SheetEnvelopeChosen(val envelopeId: String?) : BankImportEvent
    data class SheetOtherAccountChosen(val accountId: String?) : BankImportEvent
    data class SheetPendingChosen(val key: String?) : BankImportEvent
    data class SheetExistingChosen(val id: String?) : BankImportEvent
    data class SheetNotesChanged(val notes: String) : BankImportEvent
    data object SheetDoneClicked : BankImportEvent

    // Result
    data object UndoImportClicked : BankImportEvent
    data object SeeMovementsClicked : BankImportEvent
    data object DoneClicked : BankImportEvent

    // Leaving
    data object BackPressed : BankImportEvent
    data object CloseClicked : BankImportEvent
    data object DiscardConfirmed : BankImportEvent
    data object DiscardCancelled : BankImportEvent
    data object NavigationHandled : BankImportEvent
}
