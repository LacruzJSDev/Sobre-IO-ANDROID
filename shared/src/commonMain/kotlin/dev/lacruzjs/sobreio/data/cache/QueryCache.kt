package dev.lacruzjs.sobreio.data.cache

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** One read of the database: its name and every argument that changes the answer. */
data class QueryKey(val name: String, val args: List<Any?>)

/**
 * The app's memory of the last answer to every read, so a screen shown again paints at once instead of a spinner.
 *
 * - A value younger than [maxAge] and not invalidated since is **fresh**: [get] returns it without asking the database.
 * - Any write (seen by [cacheInvalidationPlugin] on the HTTP client) or a realtime change **invalidates** everything:
 *   values stay, so a screen can still paint them, but the next [get] asks the database.
 * - A change of user **clears** everything: nothing of one account is ever shown to another.
 * - Two screens asking the same key at once make one request: the second waits for the first and takes its value.
 *
 * Inside a [CacheOnlyPass] (see `ui/loading/Revalidate.kt`) [get] never touches the network: it returns the stored
 * value whatever its age and notes when it was not fresh, or throws [NotCachedException] when there is none.
 *
 * Failures are never stored. Only memory: no offline mode (out of scope).
 */
class QueryCache(
    private val maxAge: Duration = DEFAULT_MAX_AGE,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private class Entry(val value: Any?, val storedAt: TimeMark, val generation: Long)

    private val lock = Mutex()
    private val entries = HashMap<QueryKey, Entry>()
    private val keyLocks = HashMap<QueryKey, Mutex>()
    private var generation = 0L

    private val _invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits after every invalidation, so a screen on display can read again (realtime changes from other members). */
    val invalidations: SharedFlow<Unit> = _invalidations.asSharedFlow()

    suspend fun <T> get(name: String, vararg args: Any?, fetch: suspend () -> Result<T>): Result<T> =
        get(QueryKey(name, args.toList()), fetch)

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(key: QueryKey, fetch: suspend () -> Result<T>): Result<T> {
        currentCoroutineContext()[CacheOnlyPass]?.let { pass ->
            val (entry, fresh) = lock.withLock { entries[key]?.let { it to isFresh(it) } } ?: throw NotCachedException()
            if (!fresh) pass.sawStale = true
            return Result.success(entry.value as T)
        }
        val keyLock = lock.withLock { keyLocks.getOrPut(key) { Mutex() } }
        return keyLock.withLock {
            val cached = lock.withLock { entries[key]?.takeIf { isFresh(it) } }
            if (cached != null) return@withLock Result.success(cached.value as T)
            val startedAt = lock.withLock { generation }
            val result = fetch()
            result.onSuccess { value ->
                // Stored with the generation the request started in: a write that landed meanwhile leaves it stale
                lock.withLock { entries[key] = Entry(value, timeSource.markNow(), startedAt) }
            }
            result
        }
    }

    /** Every value becomes stale; they stay to be painted while the screen reads again. */
    suspend fun invalidateAll() {
        lock.withLock { generation++ }
        _invalidations.tryEmit(Unit)
    }

    /** Forgets everything: another user signs in, or this one signs out. */
    suspend fun clear() {
        lock.withLock {
            generation++
            entries.clear()
        }
        _invalidations.tryEmit(Unit)
    }

    private fun isFresh(entry: Entry): Boolean = entry.generation == generation && entry.storedAt.elapsedNow() < maxAge

    companion object {
        /** Decided 2026-11-18: a value is trusted for 30 s; writes and realtime changes invalidate it earlier. */
        val DEFAULT_MAX_AGE: Duration = 30.seconds
    }
}

/**
 * Marks a coroutine whose reads must come from [QueryCache] only. [sawStale] tells whether any of them was older than
 * the cache's limit or invalidated, so the caller knows it has to read again from the database.
 */
class CacheOnlyPass : AbstractCoroutineContextElement(CacheOnlyPass) {
    var sawStale: Boolean = false

    companion object Key : CoroutineContext.Key<CacheOnlyPass>
}

/**
 * Thrown by [QueryCache.get] in a [CacheOnlyPass] when a key has no value yet. It ends the pass (an `async` sibling
 * fails its scope with it, which rethrows it); repositories never see it, so it is never mapped to an AppError.
 */
class NotCachedException : IllegalStateException("Not cached yet")
