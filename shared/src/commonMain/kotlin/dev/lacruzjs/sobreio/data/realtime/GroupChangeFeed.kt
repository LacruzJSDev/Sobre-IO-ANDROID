package dev.lacruzjs.sobreio.data.realtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * What other members (or this user on another device) change in a group, live (D-55 in the DB repo): the database sends a
 * private Broadcast notice to `group:<id>` after every change, with no data. Each notice makes the cached reads stale and
 * tells the screens on display to read again.
 */
interface GroupChangeFeed {

    /**
     * Listens to [groupId]'s notices while collected (the open group keeps it collected). Notices that arrive together are
     * taken as one (a short debounce); after a reconnection everything is read again, since notices of the gap are lost.
     * Never fails: a lost connection is retried.
     */
    fun follow(groupId: String): Flow<Unit>

    /** One emission per change notice, after the cache has been invalidated. */
    val remoteChanges: SharedFlow<Unit>
}
