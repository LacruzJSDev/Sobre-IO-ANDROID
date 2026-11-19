package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountDeletionMapperTest {

    @Test
    fun knownOutcomesAreRead() {
        assertEquals(DeletionOutcome.Delete, "delete".toDeletionOutcome())
        assertEquals(DeletionOutcome.Leave, "leave".toDeletionOutcome())
        assertEquals(DeletionOutcome.Blocked, "blocked".toDeletionOutcome())
    }

    @Test
    fun anUnknownOutcomeIsTheMostCautiousOne() {
        assertEquals(DeletionOutcome.Blocked, "something-new".toDeletionOutcome())
    }
}
