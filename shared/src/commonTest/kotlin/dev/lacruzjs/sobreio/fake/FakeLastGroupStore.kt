package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.local.LastGroupStore

class FakeLastGroupStore : LastGroupStore {

    val values = mutableMapOf<String, String>()

    override fun get(userId: String): String? = values[userId]

    override fun set(userId: String, groupId: String) {
        values[userId] = groupId
    }

    override fun clear(userId: String) {
        values.remove(userId)
    }
}
