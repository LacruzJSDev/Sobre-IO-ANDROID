package dev.lacruzjs.sobreio.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class EntitySelectTest {

    private val options = listOf(
        EntityOption(null, "Unassigned"),
        EntityOption("1", "Groceries", trailing = "10,00 €"),
        EntityOption("2", "Rent"),
        EntityOption("3", "GROCERY club"),
    )

    @Test
    fun aBlankQueryKeepsEveryOption() {
        assertEquals(options, filterEntityOptions(options, ""))
        assertEquals(options, filterEntityOptions(options, "   "))
    }

    @Test
    fun filtersByContainsIgnoringCase() {
        assertEquals(listOf("1", "3"), filterEntityOptions(options, "groc").map { it.id })
        assertEquals(listOf("2"), filterEntityOptions(options, "ENT").map { it.id })
    }

    @Test
    fun trimsTheQueryAndFindsNothingWhenNothingMatches() {
        assertEquals(listOf("2"), filterEntityOptions(options, "  rent ").map { it.id })
        assertEquals(emptyList(), filterEntityOptions(options, "zzz"))
    }

    @Test
    fun theSpecialOptionWithoutIdIsFilteredLikeTheOthers() {
        assertEquals(listOf(null), filterEntityOptions(options, "unass").map { it.id })
    }
}
