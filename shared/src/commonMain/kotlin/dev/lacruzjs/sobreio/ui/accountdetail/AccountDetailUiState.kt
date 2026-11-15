package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountForecast
import dev.lacruzjs.sobreio.domain.model.AccountMovement
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import dev.lacruzjs.sobreio.domain.model.BankComparison
import dev.lacruzjs.sobreio.domain.model.PeriodFlow
import dev.lacruzjs.sobreio.domain.model.ProjectionPoint
import dev.lacruzjs.sobreio.domain.model.UpcomingItem
import dev.lacruzjs.sobreio.ui.undo.UndoMessage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

/** The two top tabs of the detail. */
enum class AccountDetailTab { Summary, Commitments }

/** How far back the balance chart looks: [months] before the group's today. */
enum class BalanceRange(val months: Int) { OneMonth(1), ThreeMonths(3), SixMonths(6), OneYear(12) }

/** A latest movement with the names the database does not give: who entered it, its envelope and the other account. */
data class MovementRowState(
    val movement: AccountMovement,
    /** Display name of the member who entered it; null for a former member. */
    val authorName: String?,
    /** Name of its envelope; null for transfers and for money outside the envelopes. */
    val envelopeName: String?,
    /** The envelope is the system "Unassigned" one, whose stored name is not shown (the UI has its own, translated). */
    val envelopeIsUnassigned: Boolean,
    /** Name of the other account of an account transfer. */
    val counterAccountName: String?,
)

/** The "Compare with bank" sheet while it is open. */
data class CompareState(
    /** What the user typed: the balance their bank shows. */
    val bankInput: String = "",
    /** The bank shows a debt: [bankInput], always typed positive, is compared as a negative balance. */
    val bankIsNegative: Boolean = false,
    /** The answer for [bankInput]; null while there is none (empty input, typing, waiting). */
    val result: BankComparison? = null,
    val isComparing: Boolean = false,
    val isAdjusting: Boolean = false,
    val inputError: StringResource? = null,
    val error: AppError? = null,
)

data class AccountDetailUiState(
    /** True until the first load ends; later refreshes keep showing the content. */
    val isLoading: Boolean = true,
    val error: AppError? = null,
    val groupName: String = "",
    /** Set once loaded; money is formatted with it. */
    val currencyCode: String = "EUR",
    /** Owner or admin of a live group: the pencil that edits the account is shown. */
    val canEdit: Boolean = false,
    val account: Account? = null,
    /** Name of the holder; null for a joint account. Only shown when [hasSeveralMembers]. */
    val holderName: String? = null,
    val hasSeveralMembers: Boolean = false,
    val range: BalanceRange = BalanceRange.ThreeMonths,
    /** The group's today and the last day of its period, from the database. */
    val today: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val balanceSeries: List<BalancePoint> = emptyList(),
    val projection: List<ProjectionPoint> = emptyList(),
    /** The balance series of a new [range] is on its way. */
    val isSeriesLoading: Boolean = false,
    /** Null for an archived account, which the forecast skips. */
    val forecast: AccountForecast? = null,
    /** Day on which the balance is lowest, when the forecast has a shortfall. */
    val shortfallDate: LocalDate? = null,
    val flows: List<PeriodFlow> = emptyList(),
    val upcoming: List<UpcomingItem> = emptyList(),
    val movements: List<MovementRowState> = emptyList(),
    /** Not null while the comparison sheet is open. */
    val compare: CompareState? = null,
    /** Failure of changing the range or undoing, shown above the content. */
    val actionError: AppError? = null,
    /** Snackbar of the last adjustment. */
    val undoMessage: UndoMessage? = null,
)
