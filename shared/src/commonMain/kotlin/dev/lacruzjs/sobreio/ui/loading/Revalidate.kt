package dev.lacruzjs.sobreio.ui.loading

import dev.lacruzjs.sobreio.data.cache.CacheOnlyPass
import dev.lacruzjs.sobreio.data.cache.NotCachedException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * How every screen loads (decided 2026-11-18): paint what the app already knows at once, then bring it up to date
 * without hiding it.
 *
 * [read] reads everything the screen needs through the repositories and writes it into the state, exactly as a plain
 * load would. It runs:
 * 1. Against the cache only ([CacheOnlyPass]): no network and, when everything is cached, no suspension either, so a
 *    ViewModel that starts it from `init` (on `Dispatchers.Main.immediate`) has its state filled before the first frame.
 *    A read with no value stops this pass; whatever it already wrote stays.
 * 2. Against the database, only when something was missing or older than the cache's limit (or invalidated by a write).
 *    [onRefreshing] is true while it runs: the screen keeps what it shows and works, with the thin bar under its
 *    header. With nothing to show yet the screen's own first-load state (a skeleton) applies as well.
 *
 * A [read] that finds the cached answers inconsistent (an id missing from a stale list) calls [stopIfCachePass] before
 * reporting it, so the database decides instead of an old copy.
 *
 * Returns true when the database was asked.
 */
suspend fun revalidate(onRefreshing: (Boolean) -> Unit, read: suspend () -> Unit): Boolean {
    val pass = CacheOnlyPass()
    val complete = try {
        withContext(pass) { read() }
        true
    } catch (_: NotCachedException) {
        false
    }
    if (complete && !pass.sawStale) return false
    onRefreshing(true)
    try {
        read()
    } finally {
        onRefreshing(false)
    }
    return true
}

/** In the cache-only pass, stops it (the database will answer instead); in the database pass, returns. */
suspend fun stopIfCachePass() {
    if (currentCoroutineContext()[CacheOnlyPass] != null) throw NotCachedException()
}
