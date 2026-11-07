package dev.lacruzjs.sobreio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Android lint's MissingTranslation does not see Compose resources, so this keeps both languages complete:
 * every key in English (default) must exist in Spain Spanish and the other way round.
 */
class StringsParityTest {

    private fun keys(folder: String): Set<String> =
        Regex("""<string name="([^"]+)"""")
            .findAll(File("src/commonMain/composeResources/$folder/strings.xml").readText())
            .map { it.groupValues[1] }
            .toSet()

    @Test
    fun englishAndSpanishHaveTheSameKeys() {
        val english = keys("values")
        val spanish = keys("values-es-rES")

        assertEquals(emptySet(), english - spanish, "Missing in Spanish")
        assertEquals(emptySet(), spanish - english, "Missing in English")
    }
}
