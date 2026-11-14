package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.data.remote.dto.InvitationDto
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Invitation
import dev.lacruzjs.sobreio.domain.model.InvitationStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsEveryColumnOfAnArchivedGroupWithAPurgeRequest() {
        val dto = json.decodeFromString<GroupDto>(
            """{"id":"g1","name":"Casa","currency":"EUR","timezone":"Europe/Lisbon","period_start_day":12,
               "color":"#3B6E8F","icon":"home","archived_at":"2026-11-01T10:00:00+00:00",
               "purge_requested_at":"2026-11-10T10:00:00.5+00:00",
               "account_group_members":[{"role":"admin"}]}""",
        )

        val group = dto.toDomain()

        assertEquals(
            Group(
                id = "g1",
                name = "Casa",
                currencyCode = "EUR",
                role = GroupRole.Admin,
                color = "#3B6E8F",
                icon = "home",
                timezone = "Europe/Lisbon",
                periodStartDay = 12,
                archivedAt = Instant.parse("2026-11-01T10:00:00Z"),
                purgeRequestedAt = Instant.parse("2026-11-10T10:00:00.5Z"),
            ),
            group,
        )
        assertTrue(group.isArchived && group.isPendingDeletion)
    }

    @Test
    fun aLiveGroupWithoutAppearanceUsesTheDefaults() {
        val dto = json.decodeFromString<GroupDto>(
            """{"id":"g1","name":"Casa","currency":"EUR","account_group_members":[{"role":"owner"}]}""",
        )

        val group = dto.toDomain()

        assertEquals(1, group.periodStartDay)
        assertEquals(null, group.color)
        assertTrue(group.canEdit && group.isOwner)
    }

    @Test
    fun mapsAnInvitationWithItsRole() {
        val dto = json.decodeFromString<InvitationDto>(
            """{"id":"i1","code":"AAAA11112222","role":"admin","expires_at":"2026-11-20T10:00:00+00:00"}""",
        )

        assertEquals(
            Invitation("i1", "AAAA11112222", GroupRole.Admin, Instant.parse("2026-11-20T10:00:00Z")),
            dto.toDomain(),
        )
    }

    @Test
    fun rolesRoundTripToTheirDatabaseValues() {
        GroupRole.entries.forEach { assertEquals(it, it.toDbValue().toGroupRole()) }
    }

    @Test
    fun mapsAnAcceptedInvitation() {
        val dto = json.decodeFromString<InvitationDto>(
            """{"id":"i1","code":"AAAA11112222","role":"member","status":"accepted","accepted_by":"u3",
               "accepted_at":"2026-11-12T10:00:00+00:00","expires_at":"2026-11-20T10:00:00+00:00"}""",
        )

        val invitation = dto.toDomain()

        assertEquals(InvitationStatus.Accepted, invitation.status)
        assertEquals("u3", invitation.acceptedBy)
        assertEquals(Instant.parse("2026-11-12T10:00:00Z"), invitation.acceptedAt)
    }

    @Test
    fun countsTheActiveOwnersOfTheListQuery() {
        val sole = json.decodeFromString<GroupDto>(
            """{"id":"g1","name":"Casa","currency":"EUR","account_group_members":[{"role":"owner"}],
               "owners":[{"user_id":"u1"}]}""",
        ).toDomain()
        val shared = json.decodeFromString<GroupDto>(
            """{"id":"g1","name":"Casa","currency":"EUR","account_group_members":[{"role":"owner"}],
               "owners":[{"user_id":"u1"},{"user_id":"u2"}]}""",
        ).toDomain()
        val unknown = json.decodeFromString<GroupDto>(
            """{"id":"g1","name":"Casa","currency":"EUR","account_group_members":[{"role":"owner"}]}""",
        ).toDomain()

        assertTrue(sole.isSoleOwner)
        assertEquals(2, shared.activeOwnerCount)
        assertEquals(false, shared.isSoleOwner)
        assertEquals(null, unknown.activeOwnerCount)
        assertEquals(false, unknown.isSoleOwner)
    }
}
