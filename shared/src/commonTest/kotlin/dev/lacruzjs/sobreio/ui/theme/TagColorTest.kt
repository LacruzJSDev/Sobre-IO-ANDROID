package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagColorTest {

    @Test
    fun hasTwentyDistinctColours() {
        assertEquals(20, TagColor.entries.size)
        assertEquals(20, TagColor.entries.map { it.hex.lowercase() }.toSet().size)
    }

    @Test
    fun isSeparateFromTheEntityPalette() {
        assertTrue(TagColor.entries.none { EntityColor.fromHex(it.hex) != null })
    }

    @Test
    fun lightHexMatchesTheLightColour() {
        TagColor.entries.forEach {
            val rgb = it.light.toArgb() and 0xFFFFFF
            assertEquals(it.hex.removePrefix("#").lowercase(), rgb.toString(16).padStart(6, '0'))
        }
    }

    @Test
    fun fromHexIgnoresCaseAndIsNullForUnknown() {
        assertEquals(TagColor.Sky, TagColor.fromHex(TagColor.Sky.hex.lowercase()))
        assertNull(TagColor.fromHex("#000000"))
        assertNull(TagColor.fromHex(null))
    }

    @Test
    fun theDefaultIsSage() {
        assertEquals(TagColor.Sage, TagColor.Default)
        assertEquals("#8FB39B", TagColor.Default.hex)
    }
}
