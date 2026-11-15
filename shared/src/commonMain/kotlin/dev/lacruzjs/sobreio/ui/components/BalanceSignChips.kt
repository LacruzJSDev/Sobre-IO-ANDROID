package dev.lacruzjs.sobreio.ui.components

import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.balance_sign_credit
import dev.lacruzjs.sobreio.generated.resources.balance_sign_debt
import org.jetbrains.compose.resources.stringResource

/**
 * The two chips "In credit" / "Debt" that give a balance typed as a positive amount its sign. Emits the chips only:
 * the caller puts them in a [ChipFormTile] or a row.
 */
@Composable
fun BalanceSignChips(isDebt: Boolean, enabled: Boolean, onChange: (isDebt: Boolean) -> Unit) {
    FormChip(
        label = stringResource(Res.string.balance_sign_credit),
        selected = !isDebt,
        enabled = enabled,
        onClick = { onChange(false) },
    )
    FormChip(
        label = stringResource(Res.string.balance_sign_debt),
        selected = isDebt,
        enabled = enabled,
        onClick = { onChange(true) },
    )
}
