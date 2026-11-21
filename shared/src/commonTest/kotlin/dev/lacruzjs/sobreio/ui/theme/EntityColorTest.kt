package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.luminance
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntityColorTest {

    @Test
    fun keepsTheTwelveStoredHexesAndHas24Distinct() {
        val stored = listOf(
            "#3B6E8F", "#2F7F7A", "#4F7A5A", "#7A7A3F", "#B08A2E", "#B8673A",
            "#A4503F", "#8E3B4A", "#8A5A83", "#5F5A99", "#5B6770", "#7A5E46",
        )
        val hexes = EntityColor.entries.map { it.hex.lowercase() }
        assertEquals(24, hexes.size)
        assertEquals(24, hexes.toSet().size)
        // Every hex ever stored still reads as a colour (Mustard was retuned, its old hex is a legacy one)
        stored.forEach { assertNotNull(EntityColor.fromHex(it), it) }
        assertEquals(EntityColor.Mustard, EntityColor.fromHex("#b08a2e"))
    }

    @Test
    fun whiteContentKeepsAtLeast3To1OnLightAndDark() {
        EntityColor.entries.forEach { color ->
            listOf(color.light, color.dark).forEach { fill ->
                val ratio = 1.05 / (fill.luminance() + 0.05)
                assertTrue(ratio >= 3.0, "${color.name} ${fill.toArgb().toUInt().toString(16)}: $ratio")
            }
        }
    }

    @Test
    fun fromHexIgnoresCase() {
        assertEquals(EntityColor.Blue, EntityColor.fromHex("#3b6e8f"))
        assertEquals(EntityColor.Brown, EntityColor.fromHex("#7A5E46"))
    }

    @Test
    fun fromHexIsNullForUnknownOrMissing() {
        assertNull(EntityColor.fromHex("#000000"))
        assertNull(EntityColor.fromHex(""))
        assertNull(EntityColor.fromHex(null))
    }

    @Test
    fun lightHexMatchesTheLightColour() {
        EntityColor.entries.forEach {
            val rgb = it.light.toArgb() and 0xFFFFFF
            assertEquals(it.hex.removePrefix("#").lowercase(), rgb.toString(16).padStart(6, '0'))
        }
    }
}
