package dev.lacruzjs.sobreio.ui.theme

import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntityColorTest {

    @Test
    fun hasTheTwelveAgreedHexesInOrder() {
        assertEquals(
            listOf(
                "#3B6E8F", "#2F7F7A", "#4F7A5A", "#7A7A3F", "#B08A2E", "#B8673A",
                "#A4503F", "#8E3B4A", "#8A5A83", "#5F5A99", "#5B6770", "#7A5E46",
            ),
            EntityColor.entries.map { it.hex },
        )
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
