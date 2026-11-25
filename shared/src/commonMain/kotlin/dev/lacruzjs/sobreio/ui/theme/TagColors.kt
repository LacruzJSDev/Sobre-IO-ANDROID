package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 14 soft colours a tag can take: a dot beside its name, so they are lighter and more pastel than [EntityColor], which
 * paints avatars with white content on top. Nine are medium pastels, four are pale ones and Stone is the neutral; any two
 * stay clearly apart (CIEDE2000 at least 12 in light and in dark, `TagColorTest`). The database stores [hex], the light
 * value (`#RRGGBB`); [dark] is a slightly deeper variant that keeps the dot visible on the dark paper. [legacyHexes] are
 * colours of earlier palettes that now read as this one. The order is a hue wheel; [Default] is Mint.
 */
enum class TagColor(
    val hex: String,
    val light: Color,
    val dark: Color,
    /** Hexes stored by earlier palettes that now read as this colour. */
    val legacyHexes: List<String> = emptyList(),
) {
    Rose("#F09DA3", Color(0xFFF09DA3), Color(0xFFDF8D94), legacyHexes = listOf("#DB9BAE", "#E0988A", "#EBB5B5")),
    Blush("#FCCDB9", Color(0xFFFCCDB9), Color(0xFFEBBDA9)),
    Apricot("#E7A874", Color(0xFFE7A874), Color(0xFFD69965), legacyHexes = listOf("#E6AE8C", "#F2B66D")),
    Butter("#FBD276", Color(0xFFFBD276), Color(0xFFEAC266), legacyHexes = listOf("#E8D68A")),
    Olive("#B7BF72", Color(0xFFB7BF72), Color(0xFFA8AF63), legacyHexes = listOf("#B2CF8B", "#D1BD7F")),
    Pistachio("#C9E2BB", Color(0xFFC9E2BB), Color(0xFFB9D2AB)),
    Mint("#76CCA6", Color(0xFF76CCA6), Color(0xFF66BC97), legacyHexes = listOf("#83CBB6", "#8FB39B", "#8FD1A8")),
    Aqua("#5ECBCC", Color(0xFF5ECBCC), Color(0xFF4CBBBC), legacyHexes = listOf("#7FC9D6")),
    Ice("#AEE3F6", Color(0xFFAEE3F6), Color(0xFF9ED2E5)),
    Sky("#7DBFF3", Color(0xFF7DBFF3), Color(0xFF6EAFE2), legacyHexes = listOf("#82B1D1")),
    Lavender("#B0AFF5", Color(0xFFB0AFF5), Color(0xFFA19FE4), legacyHexes = listOf("#98A8DE", "#A99FD6")),
    Lilac("#E7CEF6", Color(0xFFE7CEF6), Color(0xFFD7BEE5)),
    Orchid("#E49ECA", Color(0xFFE49ECA), Color(0xFFD38EBA), legacyHexes = listOf("#B993B3", "#C29AD0", "#D68FAE")),
    Stone("#B8BEC5", Color(0xFFB8BEC5), Color(0xFFA8AFB5), legacyHexes = listOf("#9DA8B2", "#B5A595")),
    ;

    companion object {
        /** The colour a new tag starts with. */
        val Default: TagColor = Mint

        /** Case-insensitive, legacy hexes included; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): TagColor? = hex?.let { value ->
            entries.firstOrNull { color ->
                color.hex.equals(value, ignoreCase = true) || color.legacyHexes.any { it.equals(value, ignoreCase = true) }
            }
        }
    }
}
