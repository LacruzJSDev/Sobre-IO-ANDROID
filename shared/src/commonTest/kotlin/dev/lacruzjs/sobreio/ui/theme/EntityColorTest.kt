package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntityColorTest {

    /** Every hex any released palette stored; each must still read as some colour. */
    private val everStored = listOf(
        "#3B6E8F", "#2F7F7A", "#4F7A5A", "#7A7A3F", "#B08A2E", "#B8673A", "#A4503F", "#8E3B4A", "#8A5A83", "#5F5A99",
        "#5B6770", "#7A5E46", "#2E4A73", "#3D84C6", "#2F6B4A", "#5A6E3A", "#A36A12", "#9C4A28", "#C0574A", "#A8325F",
        "#6B3F6E", "#4A52A0", "#40474D", "#7D6B5D", "#9A7A22",
    )

    @Test
    fun has18DistinctColoursAndEveryStoredHexStillResolves() {
        assertEquals(18, EntityColor.entries.size)
        val all = EntityColor.entries.flatMap { listOf(it.hex) + it.legacyHexes }.map { it.lowercase() }
        assertEquals(all.size, all.toSet().size, "a hex belongs to two colours")
        everStored.forEach { assertNotNull(EntityColor.fromHex(it), it) }
    }

    @Test
    fun anyTwoColoursAreClearlyApartInLightAndDark() {
        val light = closestPair(EntityColor.entries.map { it.name to it.light })
        val dark = closestPair(EntityColor.entries.map { it.name to it.dark })
        assertTrue(light.second >= 12.0, "too close in light: $light")
        assertTrue(dark.second >= 12.0, "too close in dark: $dark")
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
    fun fromHexIgnoresCaseAndReadsLegacyHexes() {
        assertEquals(EntityColor.Blue, EntityColor.fromHex(EntityColor.Blue.hex.lowercase()))
        assertEquals(EntityColor.Olive, EntityColor.fromHex("#b08a2e"))
        assertEquals(EntityColor.Violet, EntityColor.fromHex("#5F5A99"))
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
