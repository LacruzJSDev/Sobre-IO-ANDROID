package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.local.BankFormatStore
import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping

class FakeBankFormatStore : BankFormatStore {
    val saved = mutableMapOf<Pair<String, String>, ColumnMapping>()

    override fun get(accountId: String, headerSignature: String): ColumnMapping? = saved[accountId to headerSignature]

    override fun set(accountId: String, headerSignature: String, mapping: ColumnMapping) {
        saved[accountId to headerSignature] = mapping
    }
}
