package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 10 soft colours a tag can take: a dot beside its name, so they are lighter and more pastel than [EntityColor], which
 * paints avatars with white content on top. The database stores [hex], the light value (`#RRGGBB`); [dark] is a slightly
 * deeper variant that keeps the dot visible on the dark paper.
 */
enum class TagColor(val hex: String, val light: Color, val dark: Color) {
    Sage("#8FB39B", Color(0xFF8FB39B), Color(0xFF7FA38B)),
    Sky("#82B1D1", Color(0xFF82B1D1), Color(0xFF6FA0C2)),
    Lavender("#A99FD6", Color(0xFFA99FD6), Color(0xFF9A8FC9)),
    Rose("#DB9BAE", Color(0xFFDB9BAE), Color(0xFFCB8A9E)),
    Peach("#E6AE8C", Color(0xFFE6AE8C), Color(0xFFD99D7A)),
    Sand("#D1BD7F", Color(0xFFD1BD7F), Color(0xFFC2AE6E)),
    Mint("#83CBB6", Color(0xFF83CBB6), Color(0xFF6FBBA5)),
    Coral("#E0988A", Color(0xFFE0988A), Color(0xFFD08878)),
    Lilac("#C29AD0", Color(0xFFC29AD0), Color(0xFFB48AC2)),
    Stone("#9DA8B2", Color(0xFF9DA8B2), Color(0xFF8E99A3)),
    ;

    companion object {
        /** The colour a new tag starts with. */
        val Default: TagColor = Sage

        /** Case-insensitive; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): TagColor? = hex?.let { value -> entries.firstOrNull { it.hex.equals(value, ignoreCase = true) } }
    }
}
