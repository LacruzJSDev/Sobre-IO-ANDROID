package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.local.LastAccountStore

class FakeLastAccountStore : LastAccountStore {

    val values = mutableMapOf<Pair<String, String>, String>()

    override fun get(userId: String, groupId: String): String? = values[userId to groupId]

    override fun set(userId: String, groupId: String, accountId: String) {
        values[userId to groupId] = accountId
    }
}
