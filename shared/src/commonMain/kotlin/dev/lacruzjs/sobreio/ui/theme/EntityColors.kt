package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The 24 muted colours an entity (group, account, envelope) can take. The database stores [hex], the light value
 * (`#RRGGBB`); [dark] is a slightly lighter, softer variant that reads better on the dark paper. White content stays
 * readable on both. Tag colours are a separate palette. The order is the order
 * of the picker: a hue wheel (blues, greens, yellows, oranges, reds, purples, neutrals); it is display only.
 */
enum class EntityColor(val hex: String, val light: Color, val dark: Color) {
    Navy("#2E4A73", Color(0xFF2E4A73), Color(0xFF4B6387)),
    Blue("#3B6E8F", Color(0xFF3B6E8F), Color(0xFF5C8FB0)),
    Sky("#3D84C6", Color(0xFF3D84C6), Color(0xFF5895CE)),
    Teal("#2F7F7A", Color(0xFF2F7F7A), Color(0xFF50A09B)),
    Forest("#2F6B4A", Color(0xFF2F6B4A), Color(0xFF4C8063)),
    Green("#4F7A5A", Color(0xFF4F7A5A), Color(0xFF70987B)),
    Moss("#5A6E3A", Color(0xFF5A6E3A), Color(0xFF718256)),
    Olive("#7A7A3F", Color(0xFF7A7A3F), Color(0xFF9A9A5C)),
    Mustard("#B08A2E", Color(0xFFB08A2E), Color(0xFFC4A04D)),
    Ochre("#A36A12", Color(0xFFA36A12), Color(0xFFB07F33)),
    Orange("#B8673A", Color(0xFFB8673A), Color(0xFFCC8359)),
    Rust("#9C4A28", Color(0xFF9C4A28), Color(0xFFAA6346)),
    Terracotta("#A4503F", Color(0xFFA4503F), Color(0xFFBF6E5C)),
    Coral("#C0574A", Color(0xFFC0574A), Color(0xFFC96F63)),
    Wine("#8E3B4A", Color(0xFF8E3B4A), Color(0xFFAB5968)),
    Berry("#A8325F", Color(0xFFA8325F), Color(0xFFB44F75)),
    Mauve("#8A5A83", Color(0xFF8A5A83), Color(0xFFA67AA0)),
    Plum("#6B3F6E", Color(0xFF6B3F6E), Color(0xFF805A82)),
    Violet("#5F5A99", Color(0xFF5F5A99), Color(0xFF7F7AB8)),
    Indigo("#4A52A0", Color(0xFF4A52A0), Color(0xFF636AAD)),
    Slate("#5B6770", Color(0xFF5B6770), Color(0xFF7B8791)),
    Graphite("#40474D", Color(0xFF40474D), Color(0xFF5B6166)),
    Brown("#7A5E46", Color(0xFF7A5E46), Color(0xFF997D64)),
    Taupe("#7D6B5D", Color(0xFF7D6B5D), Color(0xFF8F8074)),
    ;

    companion object {
        /** Case-insensitive; null for no colour or one this version does not know. */
        fun fromHex(hex: String?): EntityColor? = hex?.let { value -> entries.firstOrNull { it.hex.equals(value, ignoreCase = true) } }
    }
}
