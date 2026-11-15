package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colours of money information. Only money changes in colour-blind mode (green becomes blue, red becomes orange);
 * the destructive red of `error` never does.
 */
@Immutable
data class MoneyColors(
    /** Money coming in (income, entries). */
    val inflow: Color,
    /** Money going out: the ink. */
    val outflow: Color,
    /** Transfers: money that only moves. */
    val neutral: Color,
    /** A balance below zero. */
    val negative: Color,
)

/** Which colour rule a figure follows, see `MoneyText`. */
enum class MoneyRole {
    /** A movement, due item or flow of one account: positive in, negative out. */
    Flow,

    /** A transfer in the group-wide ledger: neutral, unsigned. */
    Transfer,

    /** A balance, total, forecast or reservation: ink, red (or orange) below zero. */
    Balance,

    /** Any other figure: ink. */
    Plain,
}

/** The money colours for the light/dark and normal/colour-blind combination. */
fun moneyColors(darkTheme: Boolean, colorBlindSafe: Boolean): MoneyColors = when {
    !darkTheme && !colorBlindSafe -> MoneyColors(LightInflow, LightInk, LightMuted, LightRed)
    darkTheme && !colorBlindSafe -> MoneyColors(DarkInflow, DarkInk, DarkMuted, DarkRed)
    !darkTheme -> MoneyColors(LightInflowColorBlind, LightInk, LightMuted, LightNegativeColorBlind)
    else -> MoneyColors(DarkInflowColorBlind, DarkInk, DarkMuted, DarkNegativeColorBlind)
}

/** The colour of a figure of [role] and [amountCents]; [ink] is the plain text colour. Pure, so it can be tested. */
fun moneyColorFor(role: MoneyRole, amountCents: Long, colors: MoneyColors, ink: Color): Color = when (role) {
    MoneyRole.Flow -> if (amountCents > 0) colors.inflow else colors.outflow
    MoneyRole.Transfer -> colors.neutral
    MoneyRole.Balance -> if (amountCents < 0) colors.negative else ink
    MoneyRole.Plain -> ink
}

val LocalMoneyColors = staticCompositionLocalOf { moneyColors(darkTheme = false, colorBlindSafe = false) }
