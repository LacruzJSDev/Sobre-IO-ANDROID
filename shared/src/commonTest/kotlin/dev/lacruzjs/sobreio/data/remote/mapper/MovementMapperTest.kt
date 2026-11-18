package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.MovementSearchDto
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.MovementKind
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.MovementType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aRowOfSearchMovementsBecomesAMovement() {
        val dto = json.decodeFromString<MovementSearchDto>(
            """{"id":"m1","source":"account_transfer","account_id":"a1","counter_account_id":"a2","envelope_id":null,
               "kind":"transfer_out","amount":-1000,"date":"2026-11-12","notes":"  ","created_by":"u1",
               "created_at":"2026-11-12T10:00:00+00:00","tag_ids":["t1","t2"]}""",
        )

        val movement = dto.toDomain()

        assertEquals(MovementSource.AccountTransfer, movement.source)
        assertEquals(MovementKind.TransferOut, movement.kind)
        assertEquals(-1000, movement.amountCents)
        assertEquals(LocalDate(2026, 11, 12), movement.date)
        assertNull(movement.notes)
        assertEquals("a2", movement.counterAccountId)
        assertEquals(listOf("t1", "t2"), movement.tagIds)
        assertTrue(movement.isAccountTransfer)
        assertTrue(movement.canDelete)
    }

    @Test
    fun aTransactionRowWithoutTagsOrNotes() {
        val dto = json.decodeFromString<MovementSearchDto>(
            """{"id":"m2","source":"transaction","account_id":"a1","envelope_id":"e1","kind":"group_transfer_in",
               "amount":500,"date":"2026-11-01"}""",
        )

        val movement = dto.toDomain()

        assertEquals(MovementSource.Transaction, movement.source)
        assertEquals(MovementKind.GroupTransferIn, movement.kind)
        assertTrue(movement.tagIds.isEmpty())
        assertTrue(movement.canDelete)
        assertFalse(movement.canEdit)
    }

    @Test
    fun anEmptyFilterSendsOnlyTheGroup() {
        val params = searchParams("g1", MovementFilter())

        assertEquals(setOf("p_group_id"), params.keys)
    }

    @Test
    fun everyFilterBecomesItsParameter() {
        val filter = MovementFilter(
            accountId = "a1",
            envelopeId = "e1",
            type = MovementType.GroupTransfer,
            from = LocalDate(2026, 11, 1),
            to = LocalDate(2026, 11, 30),
            tagIds = setOf("t2", "t1"),
            createdBy = "u1",
            query = " 42,30 ",
        )

        val params = searchParams("g1", filter, limit = 50, offset = 100)

        assertEquals("a1", params.getValue("p_account_id").jsonPrimitive.content)
        assertEquals("e1", params.getValue("p_envelope_id").jsonPrimitive.content)
        assertEquals(JsonArray(listOf(JsonPrimitive("group_transfer"))), params.getValue("p_kinds"))
        assertEquals("2026-11-01", params.getValue("p_from").jsonPrimitive.content)
        assertEquals("2026-11-30", params.getValue("p_to").jsonPrimitive.content)
        assertEquals(JsonArray(listOf(JsonPrimitive("t1"), JsonPrimitive("t2"))), params.getValue("p_tag_ids"))
        assertEquals("u1", params.getValue("p_created_by").jsonPrimitive.content)
        assertEquals("42,30", params.getValue("p_query").jsonPrimitive.content)
        assertEquals("50", params.getValue("p_limit").jsonPrimitive.content)
        assertEquals("100", params.getValue("p_offset").jsonPrimitive.content)
    }

    @Test
    fun theCountTakesNoPage() {
        val params = searchParams("g1", MovementFilter(query = "rent"))

        assertFalse("p_limit" in params)
        assertFalse("p_offset" in params)
    }

    @Test
    fun aBlankQueryIsLeftOut() {
        assertFalse("p_query" in searchParams("g1", MovementFilter(query = "   ")))
    }

    @Test
    fun everyTypeHasTheNameTheDatabaseExpects() {
        assertEquals(
            listOf("expense", "income", "transfer", "adjustment", "group_transfer"),
            MovementType.entries.map { it.wireName },
        )
    }
}
