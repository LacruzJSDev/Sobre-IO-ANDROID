package dev.lacruzjs.sobreio.data.local

import com.russhwolf.settings.Settings

/** [LastAccountStore] on the platform key-value store, one key per user and group. */
class SettingsLastAccountStore(private val settings: Settings) : LastAccountStore {

    override fun get(userId: String, groupId: String): String? = settings.getStringOrNull(key(userId, groupId))

    override fun set(userId: String, groupId: String, accountId: String) = settings.putString(key(userId, groupId), accountId)

    override fun clear(userId: String) {
        val prefix = "last_account_${userId}_"
        settings.keys.filter { it.startsWith(prefix) }.forEach { settings.remove(it) }
    }

    private fun key(userId: String, groupId: String) = "last_account_${userId}_$groupId"
}
