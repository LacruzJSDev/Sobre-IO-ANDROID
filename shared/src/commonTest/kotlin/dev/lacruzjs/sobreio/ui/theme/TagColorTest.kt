package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagColorTest {

    /** Every hex any released tag palette stored; each must still read as some colour. */
    private val everStored = listOf(
        "#8FB39B", "#82B1D1", "#A99FD6", "#DB9BAE", "#E6AE8C", "#D1BD7F", "#83CBB6", "#E0988A", "#C29AD0", "#9DA8B2",
        "#98A8DE", "#7FC9D6", "#8FD1A8", "#B2CF8B", "#E8D68A", "#F2B66D", "#EBB5B5", "#D68FAE", "#B993B3", "#B5A595",
    )

    @Test
    fun has14DistinctColoursAndEveryStoredHexStillResolves() {
        assertEquals(14, TagColor.entries.size)
        val all = TagColor.entries.flatMap { listOf(it.hex) + it.legacyHexes }.map { it.lowercase() }
        assertEquals(all.size, all.toSet().size, "a hex belongs to two colours")
        everStored.forEach { assertNotNull(TagColor.fromHex(it), it) }
    }

    @Test
    fun anyTwoColoursAreClearlyApartInLightAndDark() {
        val light = closestPair(TagColor.entries.map { it.name to it.light })
        val dark = closestPair(TagColor.entries.map { it.name to it.dark })
        assertTrue(light.second >= 12.0, "too close in light: $light")
        assertTrue(dark.second >= 12.0, "too close in dark: $dark")
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
    fun fromHexIgnoresCaseReadsLegacyHexesAndIsNullForUnknown() {
        assertEquals(TagColor.Sky, TagColor.fromHex(TagColor.Sky.hex.lowercase()))
        assertEquals(TagColor.Mint, TagColor.fromHex("#8FB39B"))
        assertNull(TagColor.fromHex("#000000"))
        assertNull(TagColor.fromHex(null))
    }

    @Test
    fun theDefaultIsMint() {
        assertEquals(TagColor.Mint, TagColor.Default)
    }
}
