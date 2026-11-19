package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.realtime.GroupChangeFeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** [emit] stands for a notice from the database for the followed group. */
class FakeGroupChangeFeed : GroupChangeFeed {
    private val notices = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val _remote = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val followed = mutableListOf<String>()

    override fun follow(groupId: String): Flow<Unit> {
        followed += groupId
        return notices
    }

    override val remoteChanges: SharedFlow<Unit> = _remote.asSharedFlow()

    fun emit() {
        notices.tryEmit(Unit)
        _remote.tryEmit(Unit)
    }
}
