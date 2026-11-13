package dev.lacruzjs.sobreio.data.local

import com.russhwolf.settings.Settings

/** [LastGroupStore] on the platform key-value store (SharedPreferences, NSUserDefaults), one key per user. */
class SettingsLastGroupStore(private val settings: Settings) : LastGroupStore {

    override fun get(userId: String): String? = settings.getStringOrNull(key(userId))

    override fun set(userId: String, groupId: String) = settings.putString(key(userId), groupId)

    override fun clear(userId: String) = settings.remove(key(userId))

    private fun key(userId: String) = "last_group_$userId"
}
