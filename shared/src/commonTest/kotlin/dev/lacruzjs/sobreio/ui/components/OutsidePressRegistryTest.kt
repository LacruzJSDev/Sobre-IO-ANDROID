package dev.lacruzjs.sobreio.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

class OutsidePressRegistryTest {
    private val inside = Rect(0f, 0f, 100f, 100f)

    @Test
    fun pressOutsideClosesOnlyThoseNotUnderIt() {
        val registry = OutsidePressRegistry()
        val closed = mutableListOf<String>()
        registry.register("a", { inside }, { closed += "a" })
        registry.register("b", { Rect(200f, 0f, 300f, 100f) }, { closed += "b" })
        registry.onPress(Offset(50f, 50f))
        assertEquals(listOf("b"), closed)
    }

    @Test
    fun detachedEntryCountsAsOutside() {
        val registry = OutsidePressRegistry()
        var closed = 0
        registry.register("a", { null }, { closed++ })
        registry.onPress(Offset(1f, 1f))
        assertEquals(1, closed)
    }

    @Test
    fun unregisteredEntryIsLeftAlone() {
        val registry = OutsidePressRegistry()
        var closed = 0
        registry.register("a", { inside }, { closed++ })
        registry.unregister("a")
        registry.onPress(Offset(500f, 500f))
        assertEquals(0, closed)
    }

    @Test
    fun closingWhilePressingMayUnregisterItself() {
        val registry = OutsidePressRegistry()
        var closed = 0
        registry.register("a", { inside }, { closed++; registry.unregister("a") })
        registry.onPress(Offset(500f, 500f))
        registry.onPress(Offset(500f, 500f))
        assertEquals(1, closed)
    }
}
