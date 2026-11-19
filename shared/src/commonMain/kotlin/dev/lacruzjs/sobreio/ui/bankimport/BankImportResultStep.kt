// Bank import, Result step: what was imported, the bank balance check and Undo.

package dev.lacruzjs.sobreio.ui.bankimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.bank_import_done
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_bank_balance
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_app_balance
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_confirmed
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_created
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_difference
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_ignored
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_linked
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_matches
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_mismatch_note
import dev.lacruzjs.sobreio.generated.resources.bank_import_result_undone
import dev.lacruzjs.sobreio.generated.resources.bank_import_see_movements
import dev.lacruzjs.sobreio.generated.resources.bank_import_undo_import
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.components.StatCard
import dev.lacruzjs.sobreio.ui.components.StatRow
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import org.jetbrains.compose.resources.stringResource

// Result ---------------------------------------------------------------------------------------------------------------

@Composable
internal fun ResultStep(state: BankImportUiState, onEvent: (BankImportEvent) -> Unit) {
    val result = state.result ?: return
    StepColumn(
        bottom = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ErrorText(state.undoError)
                if (!state.undone) {
                    SecondaryButton(
                        text = stringResource(Res.string.bank_import_undo_import),
                        onClick = { onEvent(BankImportEvent.UndoImportClicked) },
                        isLoading = state.isUndoing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (result.tagId != null) {
                        PrimaryButton(
                            text = stringResource(Res.string.bank_import_see_movements),
                            onClick = { onEvent(BankImportEvent.SeeMovementsClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (state.undone || result.tagId == null) {
                    PrimaryButton(
                        text = stringResource(Res.string.bank_import_done),
                        onClick = { onEvent(BankImportEvent.DoneClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        if (state.undone) {
            // The counts described the import that no longer exists: only say what undoing did
            FormNote(stringResource(Res.string.bank_import_result_undone))
            return@StepColumn
        }
        SoftTile {
            LabelValue(stringResource(Res.string.bank_import_result_created), result.created.toString())
            LabelValue(stringResource(Res.string.bank_import_result_confirmed), result.confirmed.toString())
            LabelValue(stringResource(Res.string.bank_import_result_linked), result.linked.toString())
            LabelValue(stringResource(Res.string.bank_import_result_ignored), result.ignored.toString(), muted = true)
        }
        val comparison = state.comparison
        if (comparison != null && !state.undone) {
            val matches = comparison.differenceCents == 0L
            StatCard(
                rows = buildList {
                    add(StatRow(stringResource(Res.string.bank_import_result_bank_balance), comparison.bankBalanceCents, MoneyRole.Balance))
                    add(StatRow(stringResource(Res.string.bank_import_result_app_balance), comparison.appBalanceCents, MoneyRole.Balance))
                    if (!matches) add(StatRow(stringResource(Res.string.bank_import_result_difference), comparison.differenceCents, MoneyRole.Balance, emphasised = true))
                },
                currencyCode = state.currencyCode,
            )
            Text(
                stringResource(if (matches) Res.string.bank_import_result_matches else Res.string.bank_import_result_mismatch_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
