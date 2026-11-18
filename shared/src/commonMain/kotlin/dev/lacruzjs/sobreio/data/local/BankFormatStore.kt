package dev.lacruzjs.sobreio.data.local

import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping

/**
 * The column mapping chosen for an account's statements, per header (D-37), kept on the device so the next
 * statement of the same bank skips the Columns step.
 */
interface BankFormatStore {

    fun get(accountId: String, headerSignature: String): ColumnMapping?

    fun set(accountId: String, headerSignature: String, mapping: ColumnMapping)
}
