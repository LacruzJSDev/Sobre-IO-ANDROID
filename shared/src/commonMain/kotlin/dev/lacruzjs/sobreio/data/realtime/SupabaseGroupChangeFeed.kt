package dev.lacruzjs.sobreio.data.realtime

import dev.lacruzjs.sobreio.data.cache.QueryCache
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseGroupChangeFeed(
    private val client: SupabaseClient,
    private val cache: QueryCache,
) : GroupChangeFeed {

    private val _remoteChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val remoteChanges: SharedFlow<Unit> = _remoteChanges.asSharedFlow()

    @OptIn(FlowPreview::class)
    override fun follow(groupId: String): Flow<Unit> = channelFlow {
        val channel = client.channel("$TOPIC_PREFIX$groupId") { isPrivate = true }
        launch { channel.broadcastFlow(EVENT).collect { send(Unit) } }
        launch {
            // The first subscription reads nothing new; a later one (after a reconnection) may have missed notices
            var subscriptions = 0
            channel.status.collect { status ->
                if (status == RealtimeChannel.Status.SUBSCRIBED && ++subscriptions > 1) send(Unit)
            }
        }
        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { runCatching { client.realtime.removeChannel(channel) } }
        }
    }
        .retryWhen { _, _ ->
            delay(RETRY_DELAY_MS)
            true
        }
        .debounce(DEBOUNCE_MS)
        .onEach {
            cache.invalidateAll()
            _remoteChanges.tryEmit(Unit)
        }

    private companion object {
        const val TOPIC_PREFIX = "group:"
        const val EVENT = "change"
        const val DEBOUNCE_MS = 500L
        const val RETRY_DELAY_MS = 5_000L
    }
}
