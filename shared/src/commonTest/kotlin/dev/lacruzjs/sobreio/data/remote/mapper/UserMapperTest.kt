package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.domain.model.User
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserMapperTest {

    private fun userInfo(metadata: JsonObject?) =
        UserInfo(aud = "authenticated", id = "u1", email = "ana@example.com", userMetadata = metadata)

    @Test
    fun nameMetadataBecomesDisplayName() {
        val user = userInfo(JsonObject(mapOf("name" to JsonPrimitive("Ana")))).toDomain()

        assertEquals(User("u1", "ana@example.com", "Ana"), user)
    }

    @Test
    fun googleFullNameIsUsedWhenThereIsNoName() {
        val user = userInfo(JsonObject(mapOf("full_name" to JsonPrimitive("Ana Pérez")))).toDomain()

        assertEquals("Ana Pérez", user.displayName)
    }

    @Test
    fun nameWinsOverFullNameAndBlankNamesAreSkipped() {
        val metadata = JsonObject(mapOf("name" to JsonPrimitive(" "), "full_name" to JsonPrimitive("Ana Pérez")))

        assertEquals("Ana Pérez", userInfo(metadata).toDomain().displayName)
    }

    @Test
    fun missingMetadataGivesNoDisplayName() {
        assertNull(userInfo(null).toDomain().displayName)
    }
}
