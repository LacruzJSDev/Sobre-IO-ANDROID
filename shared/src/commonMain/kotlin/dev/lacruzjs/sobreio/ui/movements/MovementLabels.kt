package dev.lacruzjs.sobreio.ui.movements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import dev.lacruzjs.sobreio.domain.model.MovementType
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.date_preset_custom
import dev.lacruzjs.sobreio.generated.resources.date_preset_last_3_months
import dev.lacruzjs.sobreio.generated.resources.date_preset_previous_period
import dev.lacruzjs.sobreio.generated.resources.date_preset_this_period
import dev.lacruzjs.sobreio.generated.resources.date_preset_this_year
import dev.lacruzjs.sobreio.generated.resources.filter_account
import dev.lacruzjs.sobreio.generated.resources.filter_dates
import dev.lacruzjs.sobreio.generated.resources.filter_envelope
import dev.lacruzjs.sobreio.generated.resources.filter_person
import dev.lacruzjs.sobreio.generated.resources.filter_tags
import dev.lacruzjs.sobreio.generated.resources.filter_type
import dev.lacruzjs.sobreio.generated.resources.movement_type_adjustments
import dev.lacruzjs.sobreio.generated.resources.movement_type_expenses
import dev.lacruzjs.sobreio.generated.resources.movement_type_group_transfers
import dev.lacruzjs.sobreio.generated.resources.movement_type_incomes
import dev.lacruzjs.sobreio.generated.resources.movement_type_transfers
import org.jetbrains.compose.resources.StringResource

internal fun MovementType.labelRes(): StringResource = when (this) {
    MovementType.Expense -> Res.string.movement_type_expenses
    MovementType.Income -> Res.string.movement_type_incomes
    MovementType.Transfer -> Res.string.movement_type_transfers
    MovementType.Adjustment -> Res.string.movement_type_adjustments
    MovementType.GroupTransfer -> Res.string.movement_type_group_transfers
}

internal fun MovementType.icon(): ImageVector = when (this) {
    MovementType.Expense -> Icons.Filled.ArrowUpward
    MovementType.Income -> Icons.Filled.ArrowDownward
    MovementType.Transfer -> Icons.Filled.SwapHoriz
    MovementType.Adjustment -> Icons.Filled.Tune
    MovementType.GroupTransfer -> Icons.Filled.Handshake
}

internal fun DatePreset.labelRes(): StringResource = when (this) {
    DatePreset.ThisPeriod -> Res.string.date_preset_this_period
    DatePreset.PreviousPeriod -> Res.string.date_preset_previous_period
    DatePreset.Last3Months -> Res.string.date_preset_last_3_months
    DatePreset.ThisYear -> Res.string.date_preset_this_year
    DatePreset.Custom -> Res.string.date_preset_custom
}

internal fun MovementsChip.labelRes(): StringResource = when (this) {
    MovementsChip.Account -> Res.string.filter_account
    MovementsChip.Envelope -> Res.string.filter_envelope
    MovementsChip.Type -> Res.string.filter_type
    MovementsChip.Dates -> Res.string.filter_dates
    MovementsChip.Tags -> Res.string.filter_tags
    MovementsChip.Person -> Res.string.filter_person
}
