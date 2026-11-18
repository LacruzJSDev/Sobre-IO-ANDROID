package dev.lacruzjs.sobreio.ui.groupreview

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeKind
import dev.lacruzjs.sobreio.ui.format.parseMoneyInput
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/** The one-step sheet that registers a due date: expected amount and date prefilled, optional second cover. */
data class ConfirmDraft(
    val occurrence: DueOccurrence,
    val amount: String,
    val date: LocalDate,
    val coverEnvelopeId: String? = null,
    val amountError: StringResource? = null,
    val isSubmitting: Boolean = false,
    val submitError: AppError? = null,
)

data class GroupReviewUiState(
    val groupName: String = "",
    val currencyCode: String = "EUR",
    /** Any member of a live group may confirm or skip; archived groups and groups pending deletion are read only. */
    val canRecord: Boolean = false,
    /** True until the first load ends; later refreshes keep showing the content. */
    val isLoading: Boolean = true,
    /** Reading again from the database with the content on screen: the thin bar under the header. */
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    /** Due dates to review, oldest first; the ones the user just confirmed or skipped are already gone. */
    val pending: List<DueOccurrence> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val envelopes: List<Envelope> = emptyList(),
    /** The group's today; the latest date a due date can be registered on. */
    val today: LocalDate? = null,
    val sheet: ConfirmDraft? = null,
    /** Failure of skipping, undoing or reading again, shown above the list. */
    val actionError: AppError? = null,
    val undoMessage: UndoMessage? = null,
) {
    fun accountName(id: String?): String = accounts.firstOrNull { it.id == id }?.name.orEmpty()

    /**
     * Same rule as the movement form: an expense on an account inside the envelopes whose envelope (not "Unassigned") has
     * less than the amount is covered from Unassigned, so the sheet says so. Returns that envelope, or null.
     */
    fun coverShortEnvelope(draft: ConfirmDraft): Envelope? {
        val occurrence = draft.occurrence
        if (occurrence.kind != CommitmentKind.Expense) return null
        val envelope = envelopes.firstOrNull { it.id == occurrence.envelopeId } ?: return null
        if (envelope.kind == EnvelopeKind.System) return null
        if (accounts.firstOrNull { it.id == occurrence.accountId }?.inEnvelopes != true) return null
        val cents = parseMoneyInput(draft.amount)?.takeIf { it > 0 } ?: return null
        return envelope.takeIf { cents > it.balanceCents }
    }

    /** Envelopes that can also cover: live, with money, not the chosen one and not "Unassigned". */
    fun coverCandidates(draft: ConfirmDraft): List<Envelope> =
        envelopes.filter { it.kind != EnvelopeKind.System && !it.isArchived && it.id != draft.occurrence.envelopeId && it.balanceCents > 0 }
}
