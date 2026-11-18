package dev.lacruzjs.sobreio.data.local

/** The account each user recorded a movement with last, per group, kept on the device to preselect it in the form. */
interface LastAccountStore {

    fun get(userId: String, groupId: String): String?

    fun set(userId: String, groupId: String, accountId: String)

    /** Forgets every account remembered for [userId], in all groups (the account was deleted). */
    fun clear(userId: String)
}
