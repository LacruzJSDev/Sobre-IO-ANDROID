package dev.lacruzjs.sobreio.data.local

/**
 * The group each user opened last, kept on the device. Reads are synchronous on purpose: start-up decides its back
 * stack before the first frame, so there is no flash of the Groups screen.
 */
interface LastGroupStore {

    fun get(userId: String): String?

    fun set(userId: String, groupId: String)

    fun clear(userId: String)
}
