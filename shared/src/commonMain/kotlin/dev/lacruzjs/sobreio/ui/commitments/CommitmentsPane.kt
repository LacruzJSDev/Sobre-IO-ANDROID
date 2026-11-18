package dev.lacruzjs.sobreio.ui.commitments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_retry
import dev.lacruzjs.sobreio.generated.resources.commitments_empty
import dev.lacruzjs.sobreio.generated.resources.commitments_empty_editor
import dev.lacruzjs.sobreio.ui.components.EmptyState
import dev.lacruzjs.sobreio.ui.components.ListSkeleton
import dev.lacruzjs.sobreio.ui.components.UndoSnackbarHost
import dev.lacruzjs.sobreio.ui.error.messageRes
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the Commitments tab of an account: the sections and the undo snackbar. The "+" is in the detail's header and
 * the state comes from [AccountCommitmentsViewModel]; this is stateless like the other panes.
 */
@Composable
fun CommitmentsPane(
    state: AccountCommitmentsUiState,
    onEvent: (CommitmentsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The list is the only part that scrolls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            when {
                state.isLoading -> ListSkeleton(withHero = false)
                state.isEmpty && state.error != null -> EmptyState(message = stringResource(state.error.messageRes())) {
                    TextButton(onClick = { onEvent(CommitmentsEvent.Refresh) }) { Text(stringResource(Res.string.action_retry)) }
                }
                state.isEmpty -> EmptyState(
                    message = stringResource(if (state.canEdit) Res.string.commitments_empty_editor else Res.string.commitments_empty),
                )
                else -> CommitmentList(state, onEvent)
            }
        }
        UndoSnackbarHost(
            message = state.undoMessage,
            onUndo = { onEvent(CommitmentsEvent.UndoClicked) },
            onDismiss = { onEvent(CommitmentsEvent.UndoExpired) },
        )
    }
}

private val previewVersion = CommitmentVersion(
    id = "v",
    commitmentId = "c",
    effectiveFrom = LocalDate(2026, 1, 1),
    effectiveTo = null,
    kind = CommitmentKind.Expense,
    amountCents = 69_815,
    accountId = "a",
    envelopeId = "e",
    firstDueDate = LocalDate(2026, 1, 5),
    isRecurring = true,
    everyN = 1,
    unit = dev.lacruzjs.sobreio.domain.model.FrequencyUnit.Month,
    anchorDay = 5,
)

@PreviewLightDark
@Composable
private fun CommitmentsPanePreview() {
    SobreioTheme {
        CommitmentsPane(
            state = AccountCommitmentsUiState(
                rows = listOf(
                    CommitmentRow(
                        commitment = Commitment("c", "g", "Alquiler"),
                        version = previewVersion,
                        section = CommitmentSection.Expenses,
                        signedCents = -69_815,
                        nextDue = LocalDate(2026, 12, 5),
                        link = null,
                        editable = true,
                    ),
                ),
                isLoading = false,
                canEdit = true,
            ),
            onEvent = {},
        )
    }
}
