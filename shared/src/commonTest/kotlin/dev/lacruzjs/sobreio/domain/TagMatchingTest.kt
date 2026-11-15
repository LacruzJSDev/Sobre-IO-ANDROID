package dev.lacruzjs.sobreio.domain

import dev.lacruzjs.sobreio.domain.model.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagMatchingTest {

    private fun tag(id: String, name: String) = Tag(id, "g1", name)
    private val tags = listOf(tag("1", "Súper"), tag("2", "Vacaciones"), tag("3", "Reforma"), tag("4", "Gas"))

    @Test
    fun theKeyIgnoresCaseAccentsAndExtraSpaces() {
        assertEquals("super", normalizeTagKey("Súper"))
        assertEquals("super", normalizeTagKey("SUPER"))
        assertEquals("super", normalizeTagKey("  super  "))
        assertEquals("super mercado", normalizeTagKey("  Súper \t  Mercado "))
        assertEquals("nino aeo", normalizeTagKey("Niño ÀÉÖ"))
        assertEquals("", normalizeTagKey("   "))
    }

    @Test
    fun theNameIsOnlyTrimmedAndCollapsed() {
        assertEquals("Súper Mercado", normalizeTagName("  Súper   Mercado "))
    }

    @Test
    fun oneEditAwayMeansOneInsertedDeletedOrReplacedLetter() {
        assertTrue(isWithinOneEdit("reforma", "reforma"))
        assertTrue(isWithinOneEdit("reforma", "refornma"))
        assertTrue(isWithinOneEdit("reforma", "reformas"))
        assertTrue(isWithinOneEdit("reforma", "reorma"))
        assertTrue(isWithinOneEdit("reforma", "reforme"))
        assertFalse(isWithinOneEdit("reforma", "refirmo"))
        assertFalse(isWithinOneEdit("reforma", "refor"))
    }

    @Test
    fun anEmptySearchShowsEveryTagAndOffersNothing() {
        val options = tagPickerOptions(tags, "  ")

        assertEquals(tags, options.rows)
        assertNull(options.suggestion)
        assertNull(options.createName)
    }

    @Test
    fun theRowsAreTheTagsWhoseKeyContainsTheText() {
        assertEquals(listOf("1"), tagPickerOptions(tags, "UPE").rows.map { it.id })
        assertEquals(listOf("2"), tagPickerOptions(tags, "vaca").rows.map { it.id })
    }

    @Test
    fun aTextWithNoMatchOffersToCreateTheNormalisedName() {
        val options = tagPickerOptions(tags, "  Coche   nuevo ")

        assertTrue(options.rows.isEmpty())
        assertNull(options.suggestion)
        assertEquals("Coche nuevo", options.createName)
    }

    @Test
    fun anExactKeyMatchSuggestsTheTagAndDoesNotOfferToCreate() {
        val options = tagPickerOptions(tags, " super ")

        assertEquals("1", options.suggestion?.id)
        assertNull(options.createName)
    }

    @Test
    fun aTypoOfALongWordSuggestsTheTagButStillOffersToCreate() {
        val options = tagPickerOptions(tags, "Refrma")

        assertEquals("3", options.suggestion?.id)
        assertEquals("Refrma", options.createName)
    }

    @Test
    fun shortWordsAreNeverCloseToEachOther() {
        // «gas» and «gaa» differ by one letter but are too short to be a typo of each other
        val options = tagPickerOptions(tags, "gaa")

        assertNull(options.suggestion)
        assertEquals("gaa", options.createName)
    }

    @Test
    fun aNameLongerThanTheLimitCannotBeCreated() {
        assertNull(tagPickerOptions(tags, "x".repeat(MAX_TAG_NAME_LENGTH + 1)).createName)
        assertEquals("x".repeat(MAX_TAG_NAME_LENGTH), tagPickerOptions(tags, "x".repeat(MAX_TAG_NAME_LENGTH)).createName)
    }
}
