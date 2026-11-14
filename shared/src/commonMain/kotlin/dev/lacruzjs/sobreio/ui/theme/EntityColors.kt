package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 12 muted colours an entity (group, account, envelope) can take. The database stores [hex], the light value
 * (`#RRGGBB`); [dark] is a slightly lighter, softer variant that reads better on the dark paper. White content stays
 * readable on both. Tag colours are a separate palette.
 */
enum class EntityColor(val hex: String, val light: Color, val dark: Color) {
    Blue("#3B6E8F", Color(0xFF3B6E8F), Color(0xFF5C8FB0)),
    Teal("#2F7F7A", Color(0xFF2F7F7A), Color(0xFF50A09B)),
    Green("#4F7A5A", Color(0xFF4F7A5A), Color(0xFF70987B)),
    Olive("#7A7A3F", Color(0xFF7A7A3F), Color(0xFF9A9A5C)),
    Mustard("#B08A2E", Color(0xFFB08A2E), Color(0xFFC4A04D)),
    Orange("#B8673A", Color(0xFFB8673A), Color(0xFFCC8359)),
    Terracotta("#A4503F", Color(0xFFA4503F), Color(0xFFBF6E5C)),
    Wine("#8E3B4A", Color(0xFF8E3B4A), Color(0xFFAB5968)),
    Mauve("#8A5A83", Color(0xFF8A5A83), Color(0xFFA67AA0)),
    Violet("#5F5A99", Color(0xFF5F5A99), Color(0xFF7F7AB8)),
    Slate("#5B6770", Color(0xFF5B6770), Color(0xFF7B8791)),
    Brown("#7A5E46", Color(0xFF7A5E46), Color(0xFF997D64)),
    ;

    companion object {
        /** Case-insensitive; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): EntityColor? = hex?.let { value -> entries.firstOrNull { it.hex.equals(value, ignoreCase = true) } }
    }
}
