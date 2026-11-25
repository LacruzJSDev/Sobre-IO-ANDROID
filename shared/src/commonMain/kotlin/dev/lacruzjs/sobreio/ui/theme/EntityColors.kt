package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 18 muted colours an entity (group, account, envelope) can take: eight deep and eight medium hues around the wheel plus
 * two neutrals (Brown, Slate), chosen so any two stay clearly apart (CIEDE2000 at least 12 in light and in dark,
 * `EntityColorTest`) and white content keeps at least 3:1 on both. The database stores [hex], the light value
 * (`#RRGGBB`); [dark] is a lighter, softer variant for the dark paper. [legacyHexes] are colours of earlier palettes that
 * now read as this one, so stored rows keep a close colour without any data change. The order is the picker's (a hue
 * wheel, neutrals in place); it is display only. Tag colours are a separate palette.
 */
enum class EntityColor(
    val hex: String,
    val light: Color,
    val dark: Color,
    /** Hexes stored by earlier palettes that now read as this colour. */
    val legacyHexes: List<String> = emptyList(),
) {
    Wine("#823B4D", Color(0xFF823B4D), Color(0xFF965061), legacyHexes = listOf("#8E3B4A", "#A8325F")),
    Coral("#B8636E", Color(0xFFB8636E), Color(0xFFCD7983), legacyHexes = listOf("#C0574A")),
    Rust("#833E2A", Color(0xFF833E2A), Color(0xFF975440), legacyHexes = listOf("#9C4A28", "#A4503F")),
    Brown("#745B47", Color(0xFF745B47), Color(0xFF886F5B), legacyHexes = listOf("#7A5E46", "#7D6B5D")),
    Orange("#AE7033", Color(0xFFAE7033), Color(0xFFC3864E), legacyHexes = listOf("#A36A12", "#B8673A")),
    Bronze("#665405", Color(0xFF665405), Color(0xFF796828)),
    Olive("#888025", Color(0xFF888025), Color(0xFF9C9543), legacyHexes = listOf("#7A7A3F", "#9A7A22", "#B08A2E")),
    Moss("#44601C", Color(0xFF44601C), Color(0xFF587335), legacyHexes = listOf("#5A6E3A")),
    Green("#388D5F", Color(0xFF388D5F), Color(0xFF54A274), legacyHexes = listOf("#4F7A5A")),
    Forest("#0D6354", Color(0xFF0D6354), Color(0xFF307767), legacyHexes = listOf("#2F6B4A")),
    Teal("#3C8A93", Color(0xFF3C8A93), Color(0xFF579FA8), legacyHexes = listOf("#2F7F7A")),
    Petrol("#065F74", Color(0xFF065F74), Color(0xFF2C7387)),
    Blue("#4781BC", Color(0xFF4781BC), Color(0xFF5F96D0), legacyHexes = listOf("#3B6E8F", "#3D84C6")),
    Navy("#3B528D", Color(0xFF3B528D), Color(0xFF4F67A0), legacyHexes = listOf("#2E4A73", "#4A52A0")),
    Violet("#7B76BF", Color(0xFF7B76BF), Color(0xFF908BD3), legacyHexes = listOf("#5F5A99")),
    Plum("#6E4177", Color(0xFF6E4177), Color(0xFF81568A), legacyHexes = listOf("#6B3F6E")),
    Mauve("#AA6597", Color(0xFFAA6597), Color(0xFFBF7CAC), legacyHexes = listOf("#8A5A83")),
    Slate("#596571", Color(0xFF596571), Color(0xFF6D7985), legacyHexes = listOf("#40474D", "#5B6770")),
    ;

    companion object {
        /** Case-insensitive, legacy hexes included; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): EntityColor? = hex?.let { value ->
            entries.firstOrNull { color ->
                color.hex.equals(value, ignoreCase = true) || color.legacyHexes.any { it.equals(value, ignoreCase = true) }
            }
        }
    }
}
