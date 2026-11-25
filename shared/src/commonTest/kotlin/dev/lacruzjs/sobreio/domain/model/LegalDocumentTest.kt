package dev.lacruzjs.sobreio.domain.model

import dev.lacruzjs.sobreio.fake.FakeLegalRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class LegalDocumentTest {

    @Test
    fun anySpanishLocaleGetsTheSpanishTextAndEverythingElseTheEnglishOne() {
        val terms = FakeLegalRepository.TERMS
        assertEquals(terms.urlEs, terms.urlFor("es-ES"))
        assertEquals(terms.urlEs, terms.urlFor("es-MX"))
        assertEquals(terms.urlEs, terms.urlFor("ES"))
        assertEquals(terms.urlEn, terms.urlFor("en-GB"))
        assertEquals(terms.urlEn, terms.urlFor("fr-FR"))
    }
}
