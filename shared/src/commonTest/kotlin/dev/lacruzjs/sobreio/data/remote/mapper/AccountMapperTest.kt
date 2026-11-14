package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountsOverviewDto
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import dev.lacruzjs.sobreio.domain.model.AccountsSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountMapperTest {

    @Test
    fun mapsARowOfTheBalancesView() {
        val dto = AccountDto("a1", "g1", "Visa", "credit_card", inEnvelopes = false, balance = -25050)

        assertEquals(Account("a1", "g1", "Visa", AccountKind.CreditCard, false, -25050), dto.toDomain())
    }

    @Test
    fun joinsTheColourIconHolderAndArchiveDateOfTheRow() {
        val dto = AccountDto("a1", "g1", "Savings", "savings", ownerUserId = "u1", archivedAt = "2026-11-01T10:00:00Z")
        val account = dto.toDomain(AccountStyleDto("a1", color = "#4F7A5A", icon = "savings"))

        assertEquals("#4F7A5A", account.color)
        assertEquals("savings", account.icon)
        assertEquals("u1", account.ownerUserId)
        assertEquals("2026-11-01T10:00:00Z", account.archivedAt)
        assertEquals(true, account.isArchived)
    }

    @Test
    fun summaryComesFromTheOverviewAndDefaultsToZero() {
        assertEquals(AccountsSummary(900, 700, 200), AccountsOverviewDto(900, 700, 200).toSummary())
        assertEquals(AccountsSummary(0, 0, 0), (null as AccountsOverviewDto?).toSummary())
    }

    @Test
    fun kindsRoundTripThroughTheirDatabaseValue() {
        AccountKind.entries.forEach { assertEquals(it, it.toDbValue().toAccountKind()) }
        assertEquals("credit_card", AccountKind.CreditCard.toDbValue())
    }

    @Test
    fun unknownKindFallsBackToBank() {
        assertEquals(AccountKind.Bank, "crypto".toAccountKind())
        assertEquals(AccountKind.Bank, null.toAccountKind())
    }
}
