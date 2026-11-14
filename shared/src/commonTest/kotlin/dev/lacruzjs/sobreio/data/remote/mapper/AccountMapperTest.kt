package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountMapperTest {

    @Test
    fun mapsARowOfTheBalancesView() {
        val dto = AccountDto("a1", "g1", "Visa", "credit_card", inEnvelopes = false, balance = -25050)

        assertEquals(Account("a1", "g1", "Visa", AccountKind.CreditCard, false, -25050), dto.toDomain())
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
