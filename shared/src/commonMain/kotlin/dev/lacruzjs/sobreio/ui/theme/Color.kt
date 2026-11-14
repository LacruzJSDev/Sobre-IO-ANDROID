package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

// Same palette as the prototype: one app background ("paper"), one soft grey for secondary surfaces,
// a thin line (only for rare uses; lists are separated with spacing, not dividers), a muted ink for secondary text and
// a red kept for destructive actions.

// Light
val LightInk = Color(0xFF0C0C0C)
val LightPaper = Color(0xFFFFFFFF)
val LightSoft = Color(0xFFF3F3F1)
val LightLine = Color(0xFFE5E5E2)
val LightMuted = Color(0xFF6E6E6B)
val LightRed = Color(0xFFD92D20)
val LightRedSoft = Color(0xFFFEE4E2)
val LightRedInk = Color(0xFF912018)

// Dark
val DarkInk = Color(0xFFF4F4F4)
val DarkPaper = Color(0xFF171717)
val DarkSoft = Color(0xFF222221)
val DarkLine = Color(0xFF2A2A2A)
val DarkMuted = Color(0xFF9A9A96)
val DarkRed = Color(0xFFF97066)
val DarkRedSoft = Color(0xFF3B1A17)
val DarkRedInk = Color(0xFFFDA29B)

// Chart series colours (validated for colour-blind separation): blue for money in, orange for money out. Never the
// red of `error`, which is kept for destructive actions.
val LightChartIn = Color(0xFF2A78D6)
val LightChartOut = Color(0xFFEB6834)
val DarkChartIn = Color(0xFF3987E5)
val DarkChartOut = Color(0xFFD95926)
