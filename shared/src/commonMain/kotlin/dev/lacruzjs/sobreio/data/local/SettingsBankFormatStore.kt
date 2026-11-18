package dev.lacruzjs.sobreio.data.local

import com.russhwolf.settings.Settings
import dev.lacruzjs.sobreio.domain.bankimport.ColumnMapping
import kotlinx.serialization.json.Json

/** [BankFormatStore] on the platform key-value store: one key per account and header, the mapping as JSON. */
class SettingsBankFormatStore(private val settings: Settings) : BankFormatStore {

    private val json = Json { ignoreUnknownKeys = true }

    override fun get(accountId: String, headerSignature: String): ColumnMapping? =
        settings.getStringOrNull(key(accountId, headerSignature))?.let { runCatching { json.decodeFromString<ColumnMapping>(it) }.getOrNull() }

    override fun set(accountId: String, headerSignature: String, mapping: ColumnMapping) =
        settings.putString(key(accountId, headerSignature), json.encodeToString(mapping))

    // The header can be long; its hash keeps the key short and stable
    private fun key(accountId: String, headerSignature: String) = "bank_format_${accountId}_${headerSignature.hashCode().toUInt().toString(16)}"
}
