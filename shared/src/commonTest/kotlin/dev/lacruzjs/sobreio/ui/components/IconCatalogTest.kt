package dev.lacruzjs.sobreio.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IconCatalogTest {

    private fun keys(category: IconCategory) = IconCatalog.sections(category).flatMap { s -> s.icons.map { it.key } }

    @Test
    fun catalogueSizes() {
        assertEquals(20, keys(IconCategory.Group).size)
        assertEquals(20, keys(IconCategory.Account).size)
        assertEquals(32, keys(IconCategory.Envelope).size)
    }

    @Test
    fun keysAreUniquePerCategory() {
        IconCategory.entries.forEach { category ->
            val keys = keys(category)
            assertEquals(keys.size, keys.toSet().size, "Duplicate key in $category")
        }
    }

    @Test
    fun everyKeyResolvesAndKeysAreSnakeCase() {
        IconCategory.entries.forEach { category ->
            keys(category).forEach { key ->
                assertNotNull(iconFor(category, key), "$category/$key")
                assertEquals(true, Regex("[a-z]+(_[a-z]+)*").matches(key), key)
            }
        }
    }

    @Test
    fun unknownOrMissingKeyIsNull() {
        assertNull(iconFor(IconCategory.Group, "does_not_exist"))
        assertNull(iconFor(IconCategory.Group, null))
        // Belongs to another catalogue
        assertNull(iconFor(IconCategory.Group, "groceries"))
    }
}
