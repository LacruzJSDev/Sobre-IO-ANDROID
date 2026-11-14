package dev.lacruzjs.sobreio.ui.accountdetail

import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.movement_kind_adjustment
import dev.lacruzjs.sobreio.generated.resources.movement_kind_expense
import dev.lacruzjs.sobreio.generated.resources.movement_kind_group_transfer_in
import dev.lacruzjs.sobreio.generated.resources.movement_kind_group_transfer_out
import dev.lacruzjs.sobreio.generated.resources.movement_kind_income
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer_in
import dev.lacruzjs.sobreio.generated.resources.movement_kind_transfer_out
import org.jetbrains.compose.resources.StringResource

internal fun MovementKind.labelRes(): StringResource = when (this) {
    MovementKind.Income -> Res.string.movement_kind_income
    MovementKind.Expense -> Res.string.movement_kind_expense
    MovementKind.Adjustment -> Res.string.movement_kind_adjustment
    MovementKind.TransferOut -> Res.string.movement_kind_transfer_out
    MovementKind.TransferIn -> Res.string.movement_kind_transfer_in
    MovementKind.GroupTransferOut -> Res.string.movement_kind_group_transfer_out
    MovementKind.GroupTransferIn -> Res.string.movement_kind_group_transfer_in
}
