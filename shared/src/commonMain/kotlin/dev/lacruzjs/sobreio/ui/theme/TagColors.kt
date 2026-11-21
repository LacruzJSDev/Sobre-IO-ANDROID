package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 20 soft colours a tag can take: a dot beside its name, so they are lighter and more pastel than [EntityColor], which
 * paints avatars with white content on top. The database stores [hex], the light value (`#RRGGBB`); [dark] is a slightly
 * deeper variant that keeps the dot visible on the dark paper. The order is a hue wheel; [Default] is Sage, not the first.
 */
enum class TagColor(val hex: String, val light: Color, val dark: Color) {
    Sky("#82B1D1", Color(0xFF82B1D1), Color(0xFF6FA0C2)),
    Periwinkle("#98A8DE", Color(0xFF98A8DE), Color(0xFF8C9BCC)),
    Aqua("#7FC9D6", Color(0xFF7FC9D6), Color(0xFF75B9C5)),
    Mint("#83CBB6", Color(0xFF83CBB6), Color(0xFF6FBBA5)),
    Seafoam("#8FD1A8", Color(0xFF8FD1A8), Color(0xFF84C09B)),
    Sage("#8FB39B", Color(0xFF8FB39B), Color(0xFF7FA38B)),
    Pistachio("#B2CF8B", Color(0xFFB2CF8B), Color(0xFFA4BE80)),
    Butter("#E8D68A", Color(0xFFE8D68A), Color(0xFFD5C57F)),
    Sand("#D1BD7F", Color(0xFFD1BD7F), Color(0xFFC2AE6E)),
    Apricot("#F2B66D", Color(0xFFF2B66D), Color(0xFFDFA764)),
    Peach("#E6AE8C", Color(0xFFE6AE8C), Color(0xFFD99D7A)),
    Coral("#E0988A", Color(0xFFE0988A), Color(0xFFD08878)),
    Blush("#EBB5B5", Color(0xFFEBB5B5), Color(0xFFD8A7A7)),
    Rose("#DB9BAE", Color(0xFFDB9BAE), Color(0xFFCB8A9E)),
    Berry("#D68FAE", Color(0xFFD68FAE), Color(0xFFC584A0)),
    Mauve("#B993B3", Color(0xFFB993B3), Color(0xFFAA87A5)),
    Lilac("#C29AD0", Color(0xFFC29AD0), Color(0xFFB48AC2)),
    Lavender("#A99FD6", Color(0xFFA99FD6), Color(0xFF9A8FC9)),
    Stone("#9DA8B2", Color(0xFF9DA8B2), Color(0xFF8E99A3)),
    Taupe("#B5A595", Color(0xFFB5A595), Color(0xFFA79889)),
    ;

    companion object {
        /** The colour a new tag starts with. */
        val Default: TagColor = Sage

        /** Case-insensitive; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): TagColor? = hex?.let { value -> entries.firstOrNull { it.hex.equals(value, ignoreCase = true) } }
    }
}
