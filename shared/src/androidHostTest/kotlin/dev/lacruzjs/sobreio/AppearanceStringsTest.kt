package dev.lacruzjs.sobreio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every colour, icon and section the appearance code can show has a string (the parity test covers Spanish). */
class AppearanceStringsTest {

    private val strings = File("src/commonMain/composeResources/values/strings.xml").readText()
    private val sources = File("src/commonMain/kotlin/dev/lacruzjs/sobreio/ui").walkTopDown().filter { it.extension == "kt" }
        .filter { it.name == "IconCatalog.kt" || it.name == "ColorPicker.kt" }
        .joinToString("\n") { it.readText() }

    @Test
    fun everyReferencedIconColourAndSectionStringExists() {
        val referenced = Regex("""Res\.string\.((?:icon|icon_section|color)_[a-z_]+)""").findAll(sources).map { it.groupValues[1] }.toSet()
        val defined = Regex("""<string name="([^"]+)"""").findAll(strings).map { it.groupValues[1] }.toSet()

        assertEquals(true, referenced.size > 80)
        assertEquals(emptySet(), referenced - defined)
    }
}
