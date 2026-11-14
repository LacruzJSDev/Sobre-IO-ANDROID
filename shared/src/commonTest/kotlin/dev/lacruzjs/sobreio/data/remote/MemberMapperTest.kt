package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.data.remote.dto.MemberDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsAnEmbeddedProfile() {
        val dto = json.decodeFromString<MemberDto>(
            """{"user_id":"u1","role":"admin","profiles":{"display_name":"Marta"}}""",
        )

        assertEquals(Member("u1", "Marta", GroupRole.Admin), dto.toDomain())
    }

    @Test
    fun missingProfileGivesEmptyNameAndUnknownRoleIsMember() {
        val dto = json.decodeFromString<MemberDto>("""{"user_id":"u2","role":"other"}""")

        assertEquals(Member("u2", "", GroupRole.Member), dto.toDomain())
    }
}
