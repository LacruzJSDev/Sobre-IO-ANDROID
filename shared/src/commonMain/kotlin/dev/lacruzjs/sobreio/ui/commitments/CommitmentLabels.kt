package dev.lacruzjs.sobreio.ui.commitments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.commitment_kind_contribution
import dev.lacruzjs.sobreio.generated.resources.commitment_kind_expense
import dev.lacruzjs.sobreio.generated.resources.commitment_kind_income
import dev.lacruzjs.sobreio.generated.resources.commitment_kind_transfer
import dev.lacruzjs.sobreio.generated.resources.commitment_section_contributions
import dev.lacruzjs.sobreio.generated.resources.commitment_section_expenses
import dev.lacruzjs.sobreio.generated.resources.commitment_section_incomes
import dev.lacruzjs.sobreio.generated.resources.commitment_section_incoming_transfers
import dev.lacruzjs.sobreio.generated.resources.commitment_section_received_contributions
import dev.lacruzjs.sobreio.generated.resources.commitment_section_transfers
import dev.lacruzjs.sobreio.generated.resources.frequency_days
import dev.lacruzjs.sobreio.generated.resources.frequency_months
import dev.lacruzjs.sobreio.generated.resources.frequency_once
import dev.lacruzjs.sobreio.generated.resources.frequency_weeks
import dev.lacruzjs.sobreio.generated.resources.frequency_years
import dev.lacruzjs.sobreio.ui.format.formatDayMonth
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal fun CommitmentSection.titleRes(): StringResource = when (this) {
    CommitmentSection.Expenses -> Res.string.commitment_section_expenses
    CommitmentSection.Incomes -> Res.string.commitment_section_incomes
    CommitmentSection.Transfers -> Res.string.commitment_section_transfers
    CommitmentSection.IncomingTransfers -> Res.string.commitment_section_incoming_transfers
    CommitmentSection.Contributions -> Res.string.commitment_section_contributions
    CommitmentSection.ReceivedContributions -> Res.string.commitment_section_received_contributions
}

/** The kind as the user chooses it when creating: a contribution is "Contribution to another group". */
internal fun CommitmentKind.labelRes(): StringResource = when (this) {
    CommitmentKind.Expense -> Res.string.commitment_kind_expense
    CommitmentKind.Income -> Res.string.commitment_kind_income
    CommitmentKind.Transfer -> Res.string.commitment_kind_transfer
    CommitmentKind.GroupTransferOut, CommitmentKind.GroupTransferIn -> Res.string.commitment_kind_contribution
}

/** One icon per kind, drawn in ink (commitments have no colour of their own). */
internal fun CommitmentKind.icon(): ImageVector = when (this) {
    CommitmentKind.Expense -> Icons.Filled.ArrowUpward
    CommitmentKind.Income -> Icons.Filled.ArrowDownward
    CommitmentKind.Transfer -> Icons.Filled.SwapHoriz
    CommitmentKind.GroupTransferOut, CommitmentKind.GroupTransferIn -> Icons.Filled.Groups
}

/**
 * How often it is due: "Once", "Every month, day 5", "Every 2 weeks", "Every year, Dec 5"… [languageTag] writes the day
 * of a yearly one.
 */
@Composable
internal fun frequencyText(version: CommitmentVersion, languageTag: String): String {
    val unit = version.unit
    if (!version.isRecurring || unit == null) return stringResource(Res.string.frequency_once)
    val n = (version.everyN ?: 1).coerceAtLeast(1)
    return when (unit) {
        FrequencyUnit.Day -> pluralStringResource(Res.plurals.frequency_days, n, n)
        FrequencyUnit.Week -> pluralStringResource(Res.plurals.frequency_weeks, n, n)
        FrequencyUnit.Month ->
            pluralStringResource(Res.plurals.frequency_months, n, n, version.anchorDay ?: version.firstDueDate.day)
        FrequencyUnit.Year ->
            pluralStringResource(Res.plurals.frequency_years, n, n, formatDayMonth(version.firstDueDate, languageTag))
    }
}
