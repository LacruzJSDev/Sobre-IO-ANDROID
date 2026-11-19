package dev.lacruzjs.sobreio.ui.history

import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.history_field_account
import dev.lacruzjs.sobreio.generated.resources.history_field_amount
import dev.lacruzjs.sobreio.generated.resources.history_field_details
import kotlin.test.Test
import kotlin.test.assertEquals

class MovementHistorySectionTest {

    @Test
    fun bookkeepingColumnsAreLeftOutAndEachNameAppearsOnce() {
        val fields = listOf("amount", "updated_at", "updated_by", "from_account_id", "to_account_id", "kind")
        assertEquals(
            listOf(Res.string.history_field_amount, Res.string.history_field_account, Res.string.history_field_details),
            visibleHistoryFields(fields),
        )
    }

    @Test
    fun aChangeOfOnlyBookkeepingColumnsShowsNothing() {
        assertEquals(emptyList(), visibleHistoryFields(listOf("updated_at", "updated_by")))
    }
}
