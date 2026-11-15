package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.TagUsageDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun aRowOfTagUsageBecomesATagUsage() {
        val dto = json.decodeFromString<TagUsageDto>(
            """{"tag_id":"t1","name":"Vacaciones","color":"#82B1D1","archived_at":null,"movements":12}""",
        )

        val usage = dto.toDomain()

        assertEquals("t1", usage.id)
        assertEquals("Vacaciones", usage.name)
        assertEquals("#82B1D1", usage.color)
        assertEquals(12, usage.movements)
        assertFalse(usage.isArchived)
        assertEquals("g1", usage.toTag("g1").groupId)
    }

    @Test
    fun anArchivedTagWithNoColourIsArchivedAndColourless() {
        val usage = json.decodeFromString<TagUsageDto>(
            """{"tag_id":"t2","name":"Navidad","color":null,"archived_at":"2026-11-01T10:00:00+00:00","movements":0}""",
        ).toDomain()

        assertTrue(usage.isArchived)
        assertNull(usage.color)
        assertEquals(0, usage.movements)
    }
}
